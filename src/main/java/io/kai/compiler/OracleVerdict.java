package io.kai.compiler;

public sealed interface OracleVerdict permits OracleVerdict.Clean, OracleVerdict.Finding{
    record Clean() implements OracleVerdict{}
    record Finding(FindingType type, String description) implements OracleVerdict{}
}
