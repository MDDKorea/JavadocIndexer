CREATE TABLE JavadocElements
(
    qualified_name TEXT,
    full_reference TEXT,
    type           TEXT,
    value          TEXT,

    CHECK ( type = 'MODULE' OR type = 'PACKAGE' OR type = 'TYPE'
        OR type = 'FIELD' OR type = 'METHOD' ),

    PRIMARY KEY (qualified_name)
) STRICT;
