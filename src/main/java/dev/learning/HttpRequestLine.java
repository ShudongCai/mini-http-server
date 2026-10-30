package dev.learning;

public record HttpRequestLine(
        String method,
        String target,
        String version
) {
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String method;
        private String target;
        private String version;

        public Builder method(String method) {
            this.method = method;
            return this;
        }

        public Builder target(String target) {
            this.target = target;
            return this;
        }

        public Builder version(String version) {
            this.version = version;
            return this;
        }

        public HttpRequestLine build() {
            if (method == null || method.isBlank()) {
                throw new IllegalArgumentException("The method should not be null or blank");
            }

            if (target == null || target.isBlank()) {
                throw new IllegalArgumentException("The target should not be null or blank");
            }

            if (version == null || version.isBlank()) {
                throw new IllegalArgumentException("The version should not be null or blank");
            }

            return new HttpRequestLine(
                    method,
                    target,
                    version
            );
        }
    }
}
