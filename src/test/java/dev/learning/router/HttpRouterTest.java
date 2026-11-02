package dev.learning.router;

import dev.learning.request.model.HttpHeaderFields;
import dev.learning.request.model.HttpRequest;
import dev.learning.request.model.HttpRequestHeader;
import dev.learning.request.model.HttpRequestLine;
import dev.learning.response.HttpResponse;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class HttpRouterTest {
    @Test
    void shouldRouteToRegisteredHandler() {
        HttpRouter router = new HttpRouter();
        HttpRequest request = request("GET", "/hello");
        HttpResponse expected = HttpResponse.builder().setContent("Hello").build();
        List<HttpRequest> handledRequests = new ArrayList<>();
        router.register("GET", "/hello", incoming -> {
            handledRequests.add(incoming);
            return expected;
        });

        HttpResponse actual = router.route(request);

        assertSame(expected, actual);
        assertEquals(1, handledRequests.size(), "Handler must be invoked exactly once");
        assertSame(request, handledRequests.getFirst());
    }

    @Test
    void shouldMatchPathWithoutQueryString() {
        HttpRouter router = new HttpRouter();
        HttpRequest request = request("GET", "/hello?name=Jerry");
        HttpResponse expected = HttpResponse.builder().setContent("Hello Jerry").build();
        router.register("GET", "/hello", incoming -> {
            assertSame(request, incoming, "Handler must receive the original request");
            return expected;
        });

        assertSame(expected, router.route(request));
    }

    @Test
    void shouldDistinguishHttpMethods() {
        HttpRouter router = new HttpRouter();
        HttpResponse getResponse = HttpResponse.builder().setContent("GET handler").build();
        HttpResponse postResponse = HttpResponse.builder().setContent("POST handler").build();
        router.register("GET", "/hello", incoming -> getResponse);
        router.register("POST", "/hello", incoming -> postResponse);

        assertSame(getResponse, router.route(request("GET", "/hello")));
        assertSame(postResponse, router.route(request("POST", "/hello")));
    }

    @Test
    void shouldReturn404ForUnknownRoute() {
        HttpRouter router = new HttpRouter();
        router.register("GET", "/hello", incoming -> fail("Unknown route must not invoke this handler"));

        HttpResponse response = router.route(request("GET", "/missing"));

        assertNotNull(response);
        assertEquals("HTTP/1.1 404 Not Found", response.toHttpString().split("\r\n", 2)[0]);
    }

    @Test
    void shouldRejectDuplicateRoute() {
        HttpRouter router = new HttpRouter();
        HttpResponse original = HttpResponse.builder().setContent("Original handler").build();
        HttpResponse replacement = HttpResponse.builder().setContent("Replacement handler").build();
        router.register("GET", "/hello", incoming -> original);

        assertThrows(IllegalArgumentException.class,
                () -> router.register("GET", "/hello", incoming -> replacement));
        assertSame(original, router.route(request("GET", "/hello")),
                "Rejected registration must preserve the original handler");
    }

    @Test
    void shouldRejectNullHandler() {
        HttpRouter router = new HttpRouter();

        assertThrows(IllegalArgumentException.class,
                () -> router.register("GET", "/hello", null));

        HttpResponse expected = HttpResponse.builder().setContent("Valid handler").build();
        router.register("GET", "/hello", incoming -> expected);
        assertSame(expected, router.route(request("GET", "/hello")),
                "Rejected null handler must not reserve the route");
    }

    private HttpRequest request(String method, String target) {
        HttpRequestLine line = new HttpRequestLine(method, target, "HTTP/1.1");
        HttpRequestHeader header = new HttpRequestHeader(line, new HttpHeaderFields());
        return new HttpRequest(header, new byte[0]);
    }
}
