package com.ppm.integration.agilesdk.connector.octane.client;

public abstract class ValueQuery {

    protected static String parenthesis(ValueQuery q) {
        return '(' + q.toQueryString() + ')';
    }

    public static ValueQuery val(String value) {
        return new SpecifiedValue(value);
    }

    public static ValueQuery eq(String value) {
        return new Comparison("=", value);
    }

    public static ValueQuery gt(String value) {
        return new Comparison(">", value);
    }

    public static ValueQuery lt(String value) {
        return new Comparison("<", value);
    }

    public static ValueQuery ne(String value) {
        return new Comparison("<>", value);
    }

    public static ValueQuery gte(String value) {
        return new Comparison(">=", value);
    }

    public static ValueQuery lte(String value) {
        return new Comparison("<=", value);
    }

    public abstract String toQueryString();

    protected abstract int getPriority();

    static class SpecifiedValue extends ValueQuery {

        private final String value;

        SpecifiedValue(String value) {
            this.value = value;
        }

        @Override public String toQueryString() {
            String val = this.value;
            if (this.value.contains(" ")) {
                val = "\"" + val + "\"";
            }
            return val;
        }

        @Override protected int getPriority() {
            return 0;
        }
    }

    static class NotLogical extends ValueQuery {
        private final ValueQuery query;

        NotLogical(ValueQuery query) {
            this.query = query;
        }

        @Override public String toQueryString() {

            String q;
            if (this.getPriority() <= this.query.getPriority()) {
                q = parenthesis(this.query);
            } else {
                q = this.query.toQueryString();
            }

            return "NOT " + q;
        }

        @Override protected int getPriority() {
            return 10;
        }
    }

    static abstract class BinaryLogical extends ValueQuery {

        private final ValueQuery left;

        private final ValueQuery right;

        BinaryLogical(ValueQuery left, ValueQuery right) {
            this.left = left;
            this.right = right;
        }

        abstract String getOperator();

        @Override protected int getPriority() {
            return 20;
        }

        @Override public String toQueryString() {

            ValueQuery[] queries = new ValueQuery[] {this.left, this.right};
            String[] queryStr = new String[queries.length];

            for (int i = 0; i < queries.length; i++) {
                if (this.getPriority() < this.left.getPriority()) {
                    queryStr[i] = parenthesis(queries[i]);
                } else {
                    queryStr[i] = queries[i].toQueryString();
                }
            }

            return queryStr[0] + " " + this.getOperator() + " " + queryStr[1];
        }
    }

    static class ANDLogical extends BinaryLogical {

        ANDLogical(ValueQuery left, ValueQuery right) {
            super(left, right);
        }

        @Override String getOperator() {
            return " AND ";
        }
    }

    static class ORLogical extends BinaryLogical {

        ORLogical(ValueQuery left, ValueQuery right) {
            super(left, right);
        }

        @Override String getOperator() {
            return " OR ";
        }
    }

    static class Comparison extends ValueQuery {

        private final String opr;

        private final String value;

        Comparison(String opr, String value) {
            this.opr = opr;
            this.value = value;
        }

        @Override public String toQueryString() {
            String val = this.value;
            if (this.value.contains(" ")) {
                val = "\"" + val + "\"";
            }
            return this.opr + val;
        }

        @Override protected int getPriority() {
            return 1;
        }
    }
}
