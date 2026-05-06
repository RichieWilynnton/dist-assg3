package server;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ExpressionValidator {

    private static final int TARGET = 24;

    private ExpressionValidator() {}

    public static boolean validate(String expression, int[] cards) {
        try {
            Parser parser = new Parser(expression);
            Rational result = parser.parse();
            int[] used = parser.getUsedNumbers();
            return result.equals(new Rational(TARGET, 1)) && numbersMatch(used, cards);
        } catch (Exception e) {
            return false;
        }
    }

    private static boolean numbersMatch(int[] used, int[] cards) {
        if (used.length != cards.length) return false;
        int[] a = used.clone();
        int[] b = cards.clone();
        Arrays.sort(a);
        Arrays.sort(b);
        return Arrays.equals(a, b);
    }

    static final class Rational {
        final long num;
        final long den;

        Rational(long num, long den) {
            if (den == 0) throw new ArithmeticException("Division by zero");
            long g = gcd(Math.abs(num), Math.abs(den));
            this.num = (den < 0 ? -num : num) / g;
            this.den = Math.abs(den) / g;
        }

        Rational add(Rational o) { return new Rational(num * o.den + o.num * den, den * o.den); }
        Rational sub(Rational o) { return new Rational(num * o.den - o.num * den, den * o.den); }
        Rational mul(Rational o) { return new Rational(num * o.num, den * o.den); }
        Rational div(Rational o) { return new Rational(num * o.den, den * o.num); }

        boolean equals(Rational o) { return num == o.num && den == o.den; }

        private static long gcd(long a, long b) { return b == 0 ? a : gcd(b, a % b); }
    }

    static final class Parser {
        private final String input;
        private int pos;
        private final List<Integer> usedNumbers = new ArrayList<>();

        Parser(String raw) {
            this.input = raw.replaceAll("\\s+", "")
                            .replace("×", "*")
                            .replace("÷", "/")
                            .replaceAll("(?i)J", "11")
                            .replaceAll("(?i)Q", "12")
                            .replaceAll("(?i)K", "13");
        }

        int[] getUsedNumbers() {
            return usedNumbers.stream().mapToInt(i -> i).toArray();
        }

        Rational parse() {
            Rational result = parseExpr();
            if (pos != input.length()) {
                throw new RuntimeException("Unexpected character at position " + pos);
            }
            return result;
        }

        private Rational parseExpr() {
            Rational result = parseTerm();
            while (pos < input.length()) {
                char c = input.charAt(pos);
                if (c != '+' && c != '-') break;
                pos++;
                Rational right = parseTerm();
                result = c == '+' ? result.add(right) : result.sub(right);
            }
            return result;
        }

        private Rational parseTerm() {
            Rational result = parseUnary();
            while (pos < input.length()) {
                char c = input.charAt(pos);
                if (c != '*' && c != '/') break;
                pos++;
                Rational right = parseUnary();
                result = c == '*' ? result.mul(right) : result.div(right);
            }
            return result;
        }

        private Rational parseUnary() {
            if (pos < input.length() && input.charAt(pos) == '-') {
                pos++;
                return new Rational(-1, 1).mul(parseUnary());
            }
            return parseFactor();
        }

        private Rational parseFactor() {
            if (pos < input.length() && input.charAt(pos) == '(') {
                pos++;
                Rational result = parseExpr();
                if (pos >= input.length() || input.charAt(pos) != ')') {
                    throw new RuntimeException("Missing closing parenthesis");
                }
                pos++;
                return result;
            }
            return parseNumber();
        }

        private Rational parseNumber() {
            if (pos >= input.length() || !Character.isDigit(input.charAt(pos))) {
                throw new RuntimeException("Expected number at position " + pos);
            }
            int start = pos;
            while (pos < input.length() && Character.isDigit(input.charAt(pos))) pos++;
            int value = Integer.parseInt(input.substring(start, pos));
            usedNumbers.add(value);
            return new Rational(value, 1);
        }
    }
}
