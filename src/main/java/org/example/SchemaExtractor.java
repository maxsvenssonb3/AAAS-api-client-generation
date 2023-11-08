package org.example;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class SchemaExtractor {

    private static JSONObject definitions;

    public static void main(String[] args) {
        try {
            String filePath = "src/main/resources/swagger.json";
            String jsonContent = readJsonFile(filePath);

            // Replace "date-format" with "yyyy-MM-dd'T'HH:mm:ss.SSS"
            String updatedJsonContent = jsonContent.replaceAll("date-time", "yyyy-MM-dd'T'HH:mm:ss.SSS");

            // Write the updated content back to 'swagger.json'
            writeJsonFile(updatedJsonContent, filePath);

            // Now parse the updated JSON content
            JSONObject swaggerJson = new JSONObject(updatedJsonContent);

            // JSONObject swaggerJson = loadSwaggerJson("/swagger.json");
            definitions = swaggerJson.getJSONObject("components").getJSONObject("schemas");
            processPaths(swaggerJson.getJSONObject("paths"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static String readJsonFile(String filePath) throws IOException {
        return Files.readString(Path.of(filePath));
    }

    public static void writeJsonFile(String content, String filePath) throws IOException {
        Files.writeString(Path.of(filePath), content);
    }

    private static void processPaths(JSONObject paths) {
        for (String pathKey : paths.keySet()) {
            JSONObject path = paths.getJSONObject(pathKey);
            for (String methodKey : path.keySet()) {
                processMethod(path.getJSONObject(methodKey), pathKey, methodKey);
            }
        }
    }

    private static void processMethod(JSONObject method, String pathKey, String methodKey) {
        if (method.has("responses")) {
            JSONObject responses = method.getJSONObject("responses");
            for (String statusCode : responses.keySet()) {
                processResponse(responses.getJSONObject(statusCode), pathKey, methodKey, statusCode);
            }
        }
    }

    private static void processResponse(JSONObject response, String pathKey, String methodKey, String statusCode) {
        if (response.has("content") && response.getJSONObject("content").has("application/json")) {
            JSONObject jsonContent = response.getJSONObject("content").getJSONObject("application/json");
            if (jsonContent.has("schema")) {
                JSONObject schema = jsonContent.getJSONObject("schema");
                JSONObject expandedSchema = expandSchema(schema);
                JSONObject draft7Schema = addDraft7PropertiesToJsonSchema(expandedSchema);
                String fileName = String.format("%s_%s_%s_schema.json",
                    pathKey.substring(1).replaceAll("/", "_"), methodKey, statusCode);
                saveJsonToFile(draft7Schema, fileName);
            }
        }
    }

    private static JSONObject expandSchema(JSONObject schema) {
        // Check for nullability
        boolean isNullable = schema.optBoolean("nullable", false);

        // Base case: if the schema has a "$ref", then expand it and return.
        if (schema.has("$ref")) {
            String ref = schema.getString("$ref");
            String definitionName = ref.split("/")[3];
            schema = expandSchema(definitions.getJSONObject(definitionName));
        }

        // Handle "allOf" which is an array of schemas.
        if (schema.has("allOf")) {
            JSONArray allOfArray = schema.getJSONArray("allOf");
            schema.remove("allOf");  // Remove the allOf field as we will expand it.

            for (int i = 0; i < allOfArray.length(); i++) {
                JSONObject item = allOfArray.getJSONObject(i);

                // If an item inside "allOf" is itself nullable, then the overall schema is nullable.
                if (item.optBoolean("nullable", false)) {
                    isNullable = true;
                }

                // Recursively expand the schema for each item in "allOf".
                JSONObject expandedItem = expandSchema(item);

                // Merge the expanded item into the main schema.
                for (String key : expandedItem.keySet()) {
                    schema.put(key, expandedItem.get(key));
                }
            }
        }

        // Process all other nested fields recursively.
        for (String key : schema.keySet()) {
            Object value = schema.get(key);
            if (value instanceof JSONObject) {
                schema.put(key, expandSchema((JSONObject) value));
            } else if (value instanceof JSONArray) {
                for (int i = 0; i < ((JSONArray) value).length(); i++) {
                    Object item = ((JSONArray) value).get(i);
                    if (item instanceof JSONObject) {
                        ((JSONArray) value).put(i, expandSchema((JSONObject) item));
                    }
                }
            }
        }

        // Add "null" to the type if the schema is nullable.
        if (isNullable) {
            JSONArray typeArray = new JSONArray();
            typeArray.put(schema.optString("type", "object")); // Use "object" as the default type if it's missing.
            typeArray.put("null");
            schema.put("type", typeArray);
            schema.remove("nullable");
        }

        return addDraft7PropertiesToJsonSchema(schema);
    }



    // Method to add Draft 7 properties to a JSON Schema
    private static JSONObject addDraft7PropertiesToJsonSchema(JSONObject schema) {
        if (schema.has("type") && schema.has("nullable") && schema.getBoolean("nullable")) {
            JSONArray typeArray = new JSONArray();
            typeArray.put(schema.getString("type"));
            typeArray.put("null");
            schema.put("type", typeArray);
            schema.remove("nullable");
        }
        return schema;
    }

    private static void saveJsonToFile(JSONObject jsonObject, String fileName) {
        try {
            Path dirPath = Paths.get("src", "main", "resources", "schemas");
            if (Files.notExists(dirPath)) {
                Files.createDirectories(dirPath);
            }
            Path filePath = dirPath.resolve(fileName);
            Files.write(filePath, jsonObject.toString(4).getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
