package me.yassigame.sable_beyond.common;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

// i hope i will never gonna touch this again
public final class FormulaManager {
    private final Node root;

    private FormulaManager(final Node root) {
        this.root = root;
    }

    public static FormulaManager compile(final String expression) {
        if (expression == null || expression.isBlank()) {
            throw new IllegalArgumentException("Formula cannot be blank.");
        }

        return new FormulaManager(new Parser(expression).parse());
    }

    public double evaluate(final Map<String, Double> variables) {
        return this.root.evaluate(variables);
    }

    private interface Node {
        double evaluate(Map<String, Double> variables);
    }

    private record NumberNode(double value) implements Node {
        @Override
        public double evaluate(final Map<String, Double> variables) {
            return this.value;
        }
    }

    private record VariableNode(String name) implements Node {
        @Override
        public double evaluate(final Map<String, Double> variables) {
            final Double value = variables.get(this.name);
            if (value == null) {
                throw new IllegalArgumentException("Unknown variable in formula: " + this.name);
            }

            return value;
        }
    }

    private record UnaryNode(char operator, Node input) implements Node {
        @Override
        public double evaluate(final Map<String, Double> variables) {
            final double value = this.input.evaluate(variables);
            return switch (this.operator) {
                case '+' -> value;
                case '-' -> -value;
                default -> throw new IllegalStateException("Unsupported unary operator: " + this.operator);
            };
        }
    }

    private record BinaryNode(char operator, Node left, Node right) implements Node {
        @Override
        public double evaluate(final Map<String, Double> variables) {
            final double leftValue = this.left.evaluate(variables);
            final double rightValue = this.right.evaluate(variables);

            return switch (this.operator) {
                case '+' -> leftValue + rightValue;
                case '-' -> leftValue - rightValue;
                case '*' -> leftValue * rightValue;
                case '/' -> leftValue / rightValue;
                case '%' -> leftValue % rightValue;
                case '^' -> Math.pow(leftValue, rightValue);
                default -> throw new IllegalStateException("Unsupported binary operator: " + this.operator);
            };
        }
    }

    private record FunctionNode(String name, List<Node> arguments) implements Node {
        @Override
        public double evaluate(final Map<String, Double> variables) {
            final List<Double> values = new ArrayList<>(this.arguments.size());
            for (final Node argument : this.arguments) {
                values.add(argument.evaluate(variables));
            }

            return switch (this.name) {
                case "abs" -> requireArgCount(1, values, this.name, Math.abs(values.getFirst()));
                case "sqrt" -> requireArgCount(1, values, this.name, Math.sqrt(values.getFirst()));
                case "floor" -> requireArgCount(1, values, this.name, Math.floor(values.getFirst()));
                case "ceil" -> requireArgCount(1, values, this.name, Math.ceil(values.getFirst()));
                case "round" -> requireArgCount(1, values, this.name, Math.rint(values.getFirst()));
                case "min" -> requireArgCount(2, values, this.name, Math.min(values.get(0), values.get(1)));
                case "max" -> requireArgCount(2, values, this.name, Math.max(values.get(0), values.get(1)));
                case "pow" -> requireArgCount(2, values, this.name, Math.pow(values.get(0), values.get(1)));
                case "clamp" -> requireArgCount(3, values, this.name, Math.max(values.get(1), Math.min(values.get(0), values.get(2))));
                default -> throw new IllegalArgumentException("Unknown function in formula: " + this.name);
            };
        }

        private static double requireArgCount(final int expected, final List<Double> values, final String name, final double result) {
            if (values.size() != expected) {
                throw new IllegalArgumentException("Function " + name + " expects " + expected + " arguments, got " + values.size());
            }

            return result;
        }
    }

    private static final class Parser {
        private final String input;
        private int index;

        private Parser(final String input) {
            this.input = input;
        }

        private Node parse() {
            final Node node = this.parseExpression();
            this.skipWhitespace();
            if (this.hasMore()) {
                throw new IllegalArgumentException("Unexpected token in formula near index " + this.index);
            }

            return node;
        }

        // do all the hard work
        private Node parseExpression() {
            Node node = this.parseTerm();

            while (true) {
                this.skipWhitespace();
                if (this.match('+')) {
                    node = new BinaryNode('+', node, this.parseTerm());
                } else if (this.match('-')) {
                    node = new BinaryNode('-', node, this.parseTerm());
                } else {
                    return node;
                }
            }
        }

        private Node parseTerm() {
            Node node = this.parsePower();

            while (true) {
                this.skipWhitespace();
                if (this.match('*')) {
                    node = new BinaryNode('*', node, this.parsePower());
                } else if (this.match('/')) {
                    node = new BinaryNode('/', node, this.parsePower());
                } else if (this.match('%')) {
                    node = new BinaryNode('%', node, this.parsePower());
                } else {
                    return node;
                }
            }
        }

        private Node parsePower() {
            Node node = this.parseUnary();
            this.skipWhitespace();

            if (this.match('^')) {
                node = new BinaryNode('^', node, this.parsePower());
            }

            return node;
        }

        private Node parseUnary() {
            this.skipWhitespace();
            if (this.match('+')) {
                return new UnaryNode('+', this.parseUnary());
            }
            if (this.match('-')) {
                return new UnaryNode('-', this.parseUnary());
            }

            return this.parsePrimary();
        }

        private Node parsePrimary() {
            this.skipWhitespace();

            if (this.match('(')) {
                final Node node = this.parseExpression();
                this.skipWhitespace();
                if (!this.match(')')) {
                    throw new IllegalArgumentException("Missing closing ')' in formula.");
                }
                return node;
            }

            if (this.hasMore() && (Character.isDigit(this.peek()) || this.peek() == '.')) {
                return this.parseNumber();
            }

            if (this.hasMore() && isIdentifierStart(this.peek())) {
                return this.parseIdentifierOrFunction();
            }

            throw new IllegalArgumentException("Unexpected token in formula near index " + this.index);
        }

        private Node parseNumber() {
            final int start = this.index;
            while (this.hasMore() && (Character.isDigit(this.peek()) || this.peek() == '.')) {
                this.index++;
            }

            return new NumberNode(Double.parseDouble(this.input.substring(start, this.index)));
        }

        private Node parseIdentifierOrFunction() {
            final String name = this.parseIdentifier();
            this.skipWhitespace();

            if (!this.match('(')) {
                return new VariableNode(name);
            }

            final List<Node> arguments = new ArrayList<>();
            this.skipWhitespace();
            if (this.match(')')) {
                return new FunctionNode(name, arguments);
            }

            do {
                arguments.add(this.parseExpression());
                this.skipWhitespace();
            } while (this.match(','));

            if (!this.match(')')) {
                throw new IllegalArgumentException("Missing closing ')' for function " + name);
            }

            return new FunctionNode(name, arguments);
        }

        private String parseIdentifier() {
            final int start = this.index;
            this.index++;
            while (this.hasMore() && isIdentifierPart(this.peek())) {
                this.index++;
            }

            return this.input.substring(start, this.index);
        }

        private void skipWhitespace() {
            while (this.hasMore() && Character.isWhitespace(this.peek())) {
                this.index++;
            }
        }

        private boolean match(final char expected) {
            if (this.hasMore() && this.peek() == expected) {
                this.index++;
                return true;
            }

            return false;
        }

        // not peak just peek, return the current char
        private char peek() {
            return this.input.charAt(this.index);
        }

        private boolean hasMore() {
            return this.index < this.input.length();
        }

        private static boolean isIdentifierStart(final char character) {
            return Character.isLetter(character) || character == '_';
        }

        private static boolean isIdentifierPart(final char character) {
            return Character.isLetterOrDigit(character) || character == '_';
        }
    }
}
