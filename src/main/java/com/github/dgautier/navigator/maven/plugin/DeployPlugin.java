package com.github.dgautier.navigator.maven.plugin;

import com.github.dgautier.icn.exception.InvalidPluginException;
import com.github.dgautier.icn.plugin.AbstractPlugin;
import com.github.dgautier.maven.plugin.CopyTargetMojo;
import com.github.dgautier.navigator.ICNUtils;
import com.github.dgautier.navigator.model.JsonResponse;
import com.google.common.base.Preconditions;
import org.apache.http.Consts;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.CookieStore;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.protocol.ClientContext;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Mojo(name = "deployPlugin", defaultPhase = LifecyclePhase.PRE_INTEGRATION_TEST)
public class DeployPlugin extends CopyTargetMojo {

    /**
     * An ICN user with Admin rights
     */
    @Parameter(defaultValue = "p8admin", property = "adminICNUser", required = true)
    private String adminICNUser;

    /**
     * An ICN user with Admin rights password's
     */
    @Parameter(defaultValue = "p8admins", property = "adminICNPassword", required = true)
    private String adminICNPassword;

    /**
     * ICN Context Root path
     */
    @Parameter(defaultValue = "http://localhost/navigator", property = "navigatorUrl", required = true)
    private String navigatorUrl;

    @Parameter(property = "configuration", required = false)
    private String configuration;


    private static final HttpClient httpClient = new DefaultHttpClient();
    private static final HttpContext localContext = new BasicHttpContext();
    private static final CookieStore cookieStore = new BasicCookieStore();
    private String securityToken = null;


    public DeployPlugin() {
        localContext.setAttribute(ClientContext.COOKIE_STORE, cookieStore);
    }

    @Override
    public void execute() throws MojoExecutionException {
        super.execute();


        try {
            copyFileToDest();
            logonICN();
            uploadPlugin();
            savePlugin();
        } catch (IOException e) {
            throw new MojoExecutionException("Failed to copy file to Dest", e);
        } catch (InvalidPluginException e) {
            throw new MojoExecutionException("Failed to save plugin configuration", e);
        } catch (Exception e) {
            throw new MojoExecutionException(e.getMessage(), e);
        }

    }

    @Override
    protected void onCheckPreconditions() {
        super.onCheckPreconditions();
        Preconditions.checkNotNull(adminICNUser, "An ICN user with Admin rights must be set in plugin's configuration.");
        Preconditions.checkNotNull(adminICNPassword, "An ICN user password's must be set in plugin's configuration.");
        Preconditions.checkNotNull(navigatorUrl, "ICN Url must be set in plugin's configuration.");
        Preconditions.checkArgument(!navigatorUrl.endsWith("/"), "ICN Url must not be ended with /");

    }

    @Override
    protected void initConnection() {
        super.initConnection();
    }

    @Override
    protected void copyFileToDest() throws IOException {
        super.copyFileToDest();
    }

    protected JsonResponse logonICN() throws Exception {
        try {
            getLog().debug("Logging to ICN : " + navigatorUrl);
            HttpPost post = new HttpPost(navigatorUrl + "/container/logon.do");
            ICNUtils.addFormHeader(post);

            List<NameValuePair> nvps = new ArrayList<NameValuePair>();

            // FIXME desktop could be extdernalized
            nvps.add(new BasicNameValuePair("desktop", "admin"));
            nvps.add(new BasicNameValuePair("userid", adminICNUser));
            nvps.add(new BasicNameValuePair("password", adminICNPassword));


            post.setEntity(new UrlEncodedFormEntity(nvps, Consts.UTF_8));

            HttpResponse httpResponse = httpClient.execute(post, localContext);

            JsonResponse response = getResponse(httpResponse);
            Preconditions.checkNotNull(response.getSecurity_token());
            this.securityToken = response.getSecurity_token();
            getLog().debug("Security Token retrieved : " + this.securityToken);
            return response;
        } catch (Exception e) {
            getLog().error(e.getMessage(), e);
            throw e;
        }
    }

    private void checkResponseCodeOK(HttpResponse httpResponse) {
        Preconditions.checkArgument(HttpStatus.SC_OK == httpResponse.getStatusLine().getStatusCode(), "Invalid response code " + httpResponse.getStatusLine().getStatusCode());
    }

    private JsonResponse getResponse(HttpResponse httpResponse) throws IOException {
        checkResponseCodeOK(httpResponse);
        String responseEntity = EntityUtils.toString(httpResponse.getEntity());
        String responseJSON = ICNUtils.cleanResponse(responseEntity);
        JsonResponse response = ICNUtils.fromJSON(responseJSON);
        Preconditions.checkNotNull(response);

        // Ensure we have no errors
        Preconditions.checkArgument(!response.hasErrors(), response.getErrorMessage());

        // Display message if exists
        if (response.hasMessages()) {
            getLog().info(response.getInfoMessage());
        }
        return response;
    }


    /**
     * TODO use json file to load configuration from
     * @return
     * @throws Exception
     */
    protected JsonResponse savePlugin() throws Exception {
        getLog().debug("Saving plugin configuration : ");
        try {
            getLog().debug(getTargetFile().getAbsolutePath());

            AbstractPlugin plugin = ICNUtils.loadFromJar(getTargetFile());
            plugin.setFilename(getRemoteFile().getPath());


            if (this.configuration != null) {
                getLog().debug("Found configuration : " + configuration);
                plugin.setConfiguration(this.configuration);
            } else {
                getLog().debug("No plugin configuration defined.");
            }

            HttpPost httpPost = new HttpPost(navigatorUrl + "/admin/configuration.do");
            ICNUtils.addFormHeader(httpPost);

            List<NameValuePair> nvps = new ArrayList<NameValuePair>();

			/*
             * // Save plugin configuration
		/*
		"action=add" +
		"&id=PluginOpenCustomer" +
		"&configuration=PluginConfig" +
		"&application=navigator" +
		"&update_list_configuration=ApplicationConfig" +
		"&update_list_id=navigator" +
		"&update_list_type=plugins" +
		"&security_token=-1800973505737080817" +
		"&json_post=%7B%22filename%22%3A%22%2Fopt%2FIBM%2FECMClient%2Fplugins%2Fplugin-action-open-customer-0.0.4-SNAPSHOT.jar%22%2C%22name%22%3A%22Plugin%20with%20Action%20to%20Open%20New%20Customer%20Tab%22%2C%22version%22%3A%220.0.4-SNAPSHOT%22%2C%22configClass%22%3Anull%7D" +
		"&desktop=admin"

			 */
            nvps.add(new BasicNameValuePair("action", "add"));
            nvps.add(new BasicNameValuePair("id", plugin.getId()));
            nvps.add(new BasicNameValuePair("configuration", "PluginConfig"));
            nvps.add(new BasicNameValuePair("application", "navigator"));
            nvps.add(new BasicNameValuePair("update_list_configuration", "ApplicationConfig"));
            nvps.add(new BasicNameValuePair("update_list_id", "navigator"));
            nvps.add(new BasicNameValuePair("update_list_type", "plugins"));
            nvps.add(new BasicNameValuePair("security_token", this.securityToken));

            String pluginJSON = ICNUtils.getPluginJSON(plugin);
            nvps.add(new BasicNameValuePair("json_post", pluginJSON));
            nvps.add(new BasicNameValuePair("desktop", "admin"));


            httpPost.setEntity(new UrlEncodedFormEntity(nvps, Consts.UTF_8));

            HttpResponse httpResponse = httpClient.execute(httpPost, localContext);
            JsonResponse response = getResponse(httpResponse);
            return response;

        } catch (Exception e) {
            getLog().error(e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Ensure that there is no validation issue
     *
     * @return
     * @throws java.io.IOException
     * @throws InvalidPluginException
     */
    protected JsonResponse uploadPlugin() throws Exception {
        getLog().debug("Loading plugin");
        try {
            HttpPost httpPost = new HttpPost(navigatorUrl + "/admin/loadPlugin.do");
            ICNUtils.addFormHeader(httpPost);

            List<NameValuePair> nvps = new ArrayList<NameValuePair>();
            nvps.add(new BasicNameValuePair("desktop", "admin"));
            nvps.add(new BasicNameValuePair("userid", adminICNUser));
            nvps.add(new BasicNameValuePair("security_token", this.securityToken));
            getLog().debug("Remote File Path : " + getRemoteFile().getPath());
            nvps.add(new BasicNameValuePair("fileName", getRemoteFile().getPath()));

            httpPost.setEntity(new UrlEncodedFormEntity(nvps, Consts.UTF_8));

            HttpResponse httpResponse = httpClient.execute(httpPost, localContext);
            JsonResponse response = getResponse(httpResponse);
            return response;

        } catch (Exception e) {
            getLog().error(e.getMessage(), e);
            throw e;
        }
    }

    public void setAdminICNUser(String adminICNUser) {
        this.adminICNUser = adminICNUser;
    }

    public void setAdminICNPassword(String adminICNPassword) {
        this.adminICNPassword = adminICNPassword;
    }

    public void setNavigatorURL(String navigatorUrl) {
        this.navigatorUrl = navigatorUrl;
    }

    public void setConfiguration(String configuration) {
        this.configuration = configuration;
    }
}