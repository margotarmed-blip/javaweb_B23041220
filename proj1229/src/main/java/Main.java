
import base.EventBus;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import service.Api;

public class Main {
    public static void main(String[] args) throws Exception {
        Server server = new Server(80);
        ServletContextHandler handler = new ServletContextHandler();
        handler.setContextPath("/");
        handler.setResourceBase("./webapp");
        handler.addServlet(org.eclipse.jetty.servlet.DefaultServlet.class, "/");
        handler.addServlet(Api.class, "/api");
        server.setHandler(handler);
        EventBus.registerAll("service");
        server.start();
        server.join();
    }
}
