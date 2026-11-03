package dev.learning.router;

import dev.learning.request.model.HttpRequest;
import dev.learning.response.HttpResponse;
import dev.learning.router.model.RouteKey;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Routes decoded requests by case-sensitive method and exact path.
 *
 * <p>Lookup removes the query string from the request target, but performs no URL
 * decoding, path normalization or method-syntax validation. Registration and lookup
 * are not synchronized.
 */
public class HttpRouter {
    private final Map<RouteKey, Function<HttpRequest, HttpResponse>> routes;

    public HttpRouter() {
        routes = new HashMap<>();
    }

    /**
     * Invokes the matching handler with the original request, or returns a 404 response.
     *
     * <p>The handler's result is returned unchanged, and its unchecked exceptions propagate.
     *
     * @param request the request to dispatch, with a non-null header and request line
     * @return the handler's result, or a new 404 Not Found response if no route matches
     * @throws NullPointerException if the request, header or request line is null
     * @throws IllegalArgumentException if the request target is null or blank
     */
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
                    .setConnection("keep-alive")
                    .build();
        }

        Function<HttpRequest, HttpResponse> handler = routes.get(key);

        if (handler == null) {
            return HttpResponse.builder()
                    .setStatusCode(404)
                    .setReasonPhrase("Not Found")
                    .setContent("Not Found")
                    .setConnection("keep-alive")
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

    /**
     * Registers a handler for an exact method and path pair.
     *
     * <p>The method and path are stored as supplied; query strings are not removed
     * from the registered path. Existing registrations are never replaced.
     *
     * @param method the non-blank, case-sensitive method text
     * @param path the non-blank path to match
     * @param handler the non-null handler to invoke for matching requests
     * @throws IllegalArgumentException if the method or path is null or blank, the handler
     *         is null, or the same method and path are already registered
     */
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
