package com.github.dgautier.navigator;

import com.github.dgautier.icn.exception.InvalidPluginException;
import com.github.dgautier.icn.plugin.AbstractPlugin;
import com.github.dgautier.navigator.model.JsonResponse;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.http.ParseException;
import org.apache.http.client.CookieStore;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.protocol.ClientContext;
import org.apache.http.cookie.Cookie;
import org.apache.http.protocol.HttpContext;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Modifier;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.List;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

/**
 * Created by DGA on 22/01/2015.
 */
public class ICNUtils {

    public static JsonResponse fromJSON(String responseJSON) throws ParseException, IOException {
        Gson gson = new Gson();
        return gson.fromJson(responseJSON, JsonResponse.class);
    }


    public static String cleanResponse(String responseEntity) {
        String responseJSON = responseEntity.split("&&")[1];
        return responseJSON;
    }


    public static void addFormHeader(HttpPost post) {
        post.addHeader("Cache-Control", "no-cache");
        String contentType = "application/x-www-form-urlencoded; charset=UTF-8";
        post.addHeader("Content-Type", contentType);
    }


    public static AbstractPlugin loadFromJar(File pluginFile) throws InvalidPluginException {

        try {
            URL pluginURL = pluginFile.toURI().toURL();

            URL jarURL = new URL("jar:" + pluginURL.toString() + "!/");
            JarURLConnection jarLib = (JarURLConnection) jarURL.openConnection();
            JarFile jarLibFile = jarLib.getJarFile();
            Manifest jarManifest = jarLibFile.getManifest();
            Attributes jarAttributes = jarManifest.getMainAttributes();
            String pluginClassName = jarAttributes.getValue("Plugin-Class");
            jarLibFile.close();
            if (pluginClassName == null) {
                throw new InvalidPluginException("Invalid IBM Content Navigator plug-in -- missing Plugin-Class attribute in manifest.mf");
            }

            URLClassLoader pluginClassLoader = URLClassLoader.newInstance(new URL[]{pluginURL}, ICNUtils.class.getClassLoader());
            Class pluginClass = pluginClassLoader.loadClass(pluginClassName);

            AbstractPlugin plugin = (AbstractPlugin) pluginClass.newInstance();
            if (plugin != null) {
                plugin.setJarFile(jarLibFile);
            }
            return plugin;

        } catch (IllegalAccessException e) {
            throw new InvalidPluginException("Unable to load Plugin configuration from jar."

                    + System.lineSeparator() + pluginFile, e);
        } catch (InstantiationException e) {
            throw new InvalidPluginException("Unable to load Plugin configuration from jar."
                    + System.lineSeparator() + pluginFile, e);
        } catch (ClassNotFoundException e) {
            throw new InvalidPluginException("Unable to load Plugin configuration from jar."
                    + System.lineSeparator() + pluginFile, e);
        } catch (IOException e) {
            throw new InvalidPluginException("Unable to load Plugin configuration from jar."
                    + System.lineSeparator() + pluginFile, e);
        }
    }

    public static String getPluginJSON(AbstractPlugin plugin) {
        // Expected  :
        /*
         * {
		 * "filename":"/opt/IBM/ECMClient/plugins/plugin-action-open-customer-0.0.4-SNAPSHOT.jar",
		 * "name":"Plugin with Action to Open New Customer Tab",
		 * "version":"0.0.4-SNAPSHOT",
		 * "configClass":null
		 * }
		 */
        Gson gson = new GsonBuilder().excludeFieldsWithModifiers(Modifier.PRIVATE).create();
        return gson.toJson(plugin);

    }


    public static void printCookies(HttpContext localContext) {
        CookieStore cookieStore = (CookieStore) localContext.getAttribute(ClientContext.COOKIE_STORE);
        if (cookieStore != null) {
            List<Cookie> cookies = cookieStore.getCookies();
            if (cookies.isEmpty()) {
                System.out.println("None");
            } else {
                for (int i = 0; i < cookies.size(); i++) {
                    System.out.println("- " + cookies.get(i).toString());
                }
            }
        }
    }
}
