package com.ppm.integration.agilesdk.connector.octane.client;

public class FieldQuery {

    private final String field;

    private final ValueQuery query;

    public FieldQuery(String field, ValueQuery query) {
        this.field = field;
        this.query = query;
    }

    public String toQueryString() {
        return this.field + '[' + this.query.toQueryString() + ']';
    }
}
