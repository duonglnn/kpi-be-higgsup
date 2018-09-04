package com.higgsup.kpi.configure;

import com.higgsup.kpi.dto.SwaggerJson;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ResourceLoader;

import java.io.*;
import java.util.LinkedHashSet;
import java.util.Set;

@Configuration
public class SwaggerJsonConfiguration {

    @Bean
    public SwaggerJson swaggerConfiguration(
            ResourceLoader loader) throws IOException {

        InputStream istream = getResource("swagger.json", loader);
        if (istream == null) {
            throw new FileNotFoundException(
                    "Scope configuration file " + "s" + " not found.");
        }

        StringBuilder builder = new StringBuilder();
        try (BufferedReader bufferedReader
                = new BufferedReader(new InputStreamReader(istream))) {
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                builder.append(line).append("\n");
            }
        }
        String json = builder.toString();

        SwaggerJson swaggerJson = new SwaggerJson();
        swaggerJson.setJson(json);

        return swaggerJson;
    }

    /**
     * Looks for a resource at different locations in order of preference.
     *
     * @param location location of the resourcve
     * @param loader   the resource loader
     * @return input stream of the resource, null if not found
     */
    private InputStream getResource(String location, ResourceLoader loader) {

        InputStream istream = null;
        try {
            istream = loader.getResource(location).getInputStream();
        } catch (IOException e) {

        }

        if (istream == null) {
            Set<String> schemes = new LinkedHashSet<>();
            schemes.add("file:./");
            schemes.add("file:./");
            schemes.add("classpath:/");
            schemes.add("classpath:/");
            for (String scheme : schemes) {
                try {
                    istream =
                            loader.getResource(
                                    scheme + location).getInputStream();
                } catch (IOException e) {

                }
                if (istream != null) {
                    return istream;
                }
            }
        }

        return null;
    }
}