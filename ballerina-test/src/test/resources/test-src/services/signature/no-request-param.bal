import ballerina/http;

endpoint http:NonListeningService echoEP {
    port:9090
};

@http:ServiceConfig {
    basePath:"/signature"
}
service<http:Service> echo bind echoEP {
    echo1 (http:ServerConnector conn) {
        http:Response res = new;
        _ = conn -> respond(res);
    }
}
