package org.apache.jackrabbit.examples;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Calendar;
import java.util.Hashtable;

import javax.jcr.*;

import javax.naming.Context;
import javax.naming.InitialContext;

import org.apache.jackrabbit.core.jndi.RegistryHelper;

/**
 *
 */
public class FileSystemUtil {

    /**
     *
     */
    public static void main(String[] args) {
        if (args.length == 0) {
            System.out.println("Usage: [--add NODENAME] [--view] [--import DIRECTORY/FILE] --repodir REPO_DIR (e.g. --add hugo --repodir build/repotest) --workspace WORKSPACE");
            return;
        }

        String repHomeDir = null;
        String workspace = null;
        String action = null;

        // --add
        String nodeName = null;

        // --view
        String outputFormat = "xml";

        // --import
        String file = null;

        // --import-xml-file
        String xmlFile = null;

        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("--repodir")) {
                repHomeDir = args[i + 1];
            }
            if (args[i].equals("--add")) {
                action = "add";
                nodeName = args[i + 1];
            }
            if (args[i].equals("--view")) {
                action = "view";
                outputFormat = "xml";
            }
            if (args[i].equals("--import")) {
                action = "import";
                file = args[i + 1];
            }
            if (args[i].equals("--import-xml-file")) {
                action = "importxmlfile";
                xmlFile = args[i + 1];
            }
	    if (args[i].equals("--workspace")) {
                workspace = args[i+1];
            }	
        }

        if (workspace == null) {
            workspace = "default";
        }

        if (repHomeDir == null) {
            System.err.println("No Repository directory specified (--repodir)!");
            return;
        }
        if (action == null) {
            System.err.println("No valid action specified (e.g. --add or --view)!");
            return;
        } else if (action.equals("add")) {
            new FileSystemUtil().addNode(repHomeDir, workspace, nodeName);
        } else if (action.equals("view")) {
            new FileSystemUtil().view(repHomeDir, workspace, outputFormat);
        } else if (action.equals("import")) {
            new FileSystemUtil().importFromFilesystem(repHomeDir, workspace, file);
        } else if (action.equals("importxmlfile")) {
            new FileSystemUtil().importXMLFile(repHomeDir, workspace, xmlFile);
        } else {
            System.err.println("No such action implemented (e.g. --add)!");
        }

    }

    /**
     *
     */
    public void addNode(String repHomeDir, String workspace, String nodeName) {
        try {
            Repository r = getRepository(repHomeDir);
            String userId = "anonymous";
            char[] password = "".toCharArray();
            Session session = r.login(new SimpleCredentials(userId, password), workspace);
            Node rootNode = session.getRootNode();

            addNode(rootNode, nodeName, null);

            session.save();
        } catch (Exception e){
            e.printStackTrace(System.err);
            //System.err.println("EXCEPTION: " + e.getStackTrace());
        }
    }

    /**
     *
     */
    public void addNode(Node rootNode, String nodeName, File file) throws Exception {
        System.out.println(rootNode.getPrimaryNodeType().getName());


        if (!rootNode.hasNode(nodeName)) {
            System.out.println("Attempting to create node: " + nodeName);

            Node n = null;
	    if (file != null && file.isFile()) {
	        n = rootNode.addNode(nodeName, "nt:file");
                Node resNode = n.addNode("jcr:content", "nt:resource");
                resNode.setProperty("jcr:data", new FileInputStream(file));
                resNode.setProperty("jcr:encoding", "");
                resNode.setProperty("jcr:mimeType", "");
                Calendar lastModified = Calendar.getInstance();
                lastModified.setTimeInMillis(file.lastModified());
                resNode.setProperty("jcr:lastModified", lastModified);
            } else {
                n = rootNode.addNode(nodeName, "nt:unstructured");
                String propertyValue = nodeName;
                int lastIndex = nodeName.lastIndexOf("/");
                if (lastIndex > -1) {
                    propertyValue = nodeName.substring(lastIndex + 1);
                }
                String propertyName = "name";
                n.setProperty(propertyName, new StringValue(propertyValue));
                //System.out.println("Property value: " + rootNode.getProperty(nodeName + "/" + propertyName).getString());
            }

        } else {
            System.out.println("Node does already exist: " + nodeName);
        }
    }

    /**
     *
     */
    public void view(String repHomeDir, String workspace, String outputFormat) {
        System.out.println("View repository: " + repHomeDir + ", workspace: " + workspace);
        try {
            Repository r = getRepository(repHomeDir);
            String userId = "anonymous";
            char[] password = "".toCharArray();
            Session session = r.login(new SimpleCredentials(userId, password), workspace);

            String[] prefixes = session.getNamespacePrefixes();
            for (int i = 0; i < prefixes.length; i++) {
                String namespace = session.getNamespaceURI(prefixes[i]);
                System.out.println("Namespace Prefix: " + prefixes[i]);
                System.out.println("Namespace: " + namespace);
            }

            Node rootNode = session.getRootNode();
								          
            dump(rootNode);
            session.save();
        } catch (Exception e){
            e.printStackTrace(System.err);
            //System.err.println("EXCEPTION: " + e.getStackTrace());
        }
    }

    /**
     *
     */
    public static void dump (Node n) throws RepositoryException {
        System.out.println("Node: " + n.getPath());
        PropertyIterator pit=n.getProperties();
        while (pit.hasNext()) {
            Property p = pit.nextProperty();
	    if (p.getDefinition().isMultiple()) {
                Value[] values = p.getValues();
                String vs = "";
                for (int i = 0; i < values.length; i++) {
                    if (i > 0) vs = vs + ":";
                    vs = vs + values[i].getString();
                }
                if (values.length == 0) vs = "NO VALUES!";
                System.out.println("Property with multiple values: " + p.getPath() + " = " + vs);
            } else {
                System.out.println("Property: " + p.getPath() + " = " + p.getString());
            }
        }
        NodeIterator nit=n.getNodes();
        while (nit.hasNext()) {
            Node cn=nit.nextNode();
            dump (cn);
        }
    }

    /**
     * Import data from filesystem
     */
    public void importFromFilesystem(String repHomeDir, String workspace, String filename) {
        try {
            Repository r = getRepository(repHomeDir);
            String userId = "anonymous";
            char[] password = "".toCharArray();
            Session session = r.login(new SimpleCredentials(userId, password), workspace);
            Node rootNode = session.getRootNode();
								          
            System.out.println(rootNode.getPrimaryNodeType().getName());

            String propertyName = "name";

            File file = new File(filename);
	    if (!file.isAbsolute()) {
                file = new File(System.getProperty("user.dir"), filename);
            }

            browse(file, file.getParent(), rootNode);

            session.save();
        } catch (Exception e){
            e.printStackTrace(System.err);
            //System.err.println("EXCEPTION: " + e.getStackTrace());
        }
    }

    /**
     *
     */
    private void browse(File file, String offset, Node rootNode) throws Exception {
        if (!file.exists()) {
            System.err.println("No such file or directory: " + file);
            return;
        }

        String nodePath = file.getAbsolutePath().substring(offset.length() + 1);
        addNode(rootNode, nodePath, file);

        if (file.isDirectory()) {
            //System.out.println("Directory: " + file);
            System.out.println("Directory: " + nodePath);
            String[] filesAndDirs = file.list();
            for (int i = 0;i < filesAndDirs.length;i++) {
                browse(new File(file, filesAndDirs[i]), offset, rootNode);
            }
        } else if (file.isFile()) {
            //System.out.println("File: " + file);
            System.out.println("File: " + nodePath);
        } else {
            System.err.println("Neither file nor directory: " + file);
        }
    }

    /**
     * Import XML file
     */
    public void importXMLFile(String repHomeDir, String workspace, String filename) {
        try {
            Repository r = getRepository(repHomeDir);
            String userId = "anonymous";
            char[] password = "".toCharArray();
            Session session = r.login(new SimpleCredentials(userId, password), workspace);
            Node rootNode = session.getRootNode();
								          
            System.out.println(rootNode.getPrimaryNodeType().getName());

            String nodeName = "importxmlfile";
            Node node = rootNode.addNode(nodeName, "nt:unstructured");
            session.importXML("/" + nodeName, new FileInputStream(filename));
	    
            session.save();
        } catch (Exception e){
            e.printStackTrace(System.err);
            //System.err.println("EXCEPTION: " + e.getStackTrace());
        }
    }

    /**
     *
     */
    private Repository getRepository(String repHomeDir) throws Exception {
        String configFile = repHomeDir + File.separator + "repository.xml";
				        
        Hashtable env = new Hashtable();
        env.put(Context.INITIAL_CONTEXT_FACTORY, "org.apache.jackrabbit.core.jndi.provider.DummyInitialContextFactory");
        env.put(Context.PROVIDER_URL, "localhost");
        InitialContext ctx = new InitialContext(env);
					          
        RegistryHelper.registerRepository(ctx, "repo", configFile, repHomeDir, true);
        return (Repository) ctx.lookup("repo");
    }
}
