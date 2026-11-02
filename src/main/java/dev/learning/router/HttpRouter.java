package dev.learning.router;

import dev.learning.request.model.HttpRequest;
import dev.learning.response.HttpResponse;
import dev.learning.router.model.RouteKey;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public class HttpRouter {
    private final Map<RouteKey, Function<HttpRequest, HttpResponse>> routes;

    public HttpRouter() {
        routes = new HashMap<>();
    }

    public HttpResponse route(HttpRequest request) {
        String method = request.getHttpRequestHeader().requestLine().method();
        String target = request.getHttpRequestHeader().requestLine().target();

        validateMethod(method);
        String path = extractPath(target);

        RouteKey key = new RouteKey(method, path);
        if (!routes.containsKey(key)) {
            return HttpResponse.builder()
                    .setStatusCode(404)
                    .setReasonPhrase("Not Found")
                    .setContent("Not Found")
                    .build();
        }

        Function<HttpRequest, HttpResponse> handler = routes.get(key);

        if (handler == null) {
            return HttpResponse.builder()
                    .setStatusCode(404)
                    .setReasonPhrase("Not Found")
                    .setContent("Not Found")
                    .build();
        }

        return handler.apply(request);
    }

    private void validateMethod(String method) {
        //Do this later, validate if the Method is valid
    }

    private String extractPath(String target) {
        if (target == null || target.isBlank()) {
            throw new IllegalArgumentException("Invalid request target");
        }

        int questionIndex = target.indexOf('?');

        return questionIndex == -1
                ? target
                : target.substring(0, questionIndex);
    }

//    private void validateAndSpiltPath(String target) {
//        if (target == null || target.isBlank()) {
//            throw new IllegalArgumentException("Path should not be null or blank");
//        }
//
//        int questionIndex = target.indexOf('?');
//        String path = questionIndex == -1 ?
//                target :
//                target.substring(0, questionIndex);
//
//        String query = questionIndex == -1
//                ? ""
//                : target.substring(questionIndex + 1);
//
//        String[] parameters = query.split("&");
//        String[][] parameterMap = new String[parameters.length][2];
//        for (int i = 0; i < parameters.length; ++i) {
//            String parameter = parameters[i];
//            int indexOfEqual = parameter.indexOf('=');
//
//            if (indexOfEqual == -1) {
//                parameterMap[i][0] = parameter;
//                parameterMap[i][1] = "";
//            } else {
//                parameterMap[i][0] = parameter.substring(0, indexOfEqual);
//                parameterMap[i][1] = parameter.substring(indexOfEqual + 1);
//            }
//        }
//    }

    public void register(String method,
                         String path,
                         Function<HttpRequest, HttpResponse> handler) {
        if (method == null || method.isBlank()) {
            throw new IllegalArgumentException("Method should not be null or blank");
        }

        if (path == null || path.isBlank()) {
            throw new IllegalArgumentException("Path should not be null or blank");
        }

        if (handler == null) {
            throw new IllegalArgumentException("Handler should not be null");
        }

        RouteKey routeKey = new RouteKey(method, path);

        if (routes.containsKey(routeKey)) {
            throw new IllegalArgumentException("Should not register the same Path and Method");
        }

        routes.put(routeKey, handler);
    }
}
