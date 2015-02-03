tokens = %w(
ADD
AFTER
ALTER
BIT
COLLATE
COLUMN
ENGINE
FIRST
IGNORE
MEDIUMINT
ONLINE
OFFLINE
SMALLINT
TABLE
TINYINT

UNSIGNED
ZEROFILL
BIT
TINYINT
CHARACTER
SET
COLLATE
SMALLINT
MEDIUMINT
INT
INTEGER
BIGINT
REAL
DOUBLE
FLOAT
DECIMAL
NUMERIC
DATE
TIME
TIMESTAMP
DATETIME
YEAR
CHAR
VARCHAR
BINARY
VARBINARY
TINYBLOB
BLOB
MEDIUMBLOB
LONGBLOB
TINYTEXT
TEXT
MEDIUMTEXT
LONGTEXT
ENUM
SET

NOT
NULL
DEFAULT

AUTO_INCREMENT
UNIQUE
PRIMARY
KEY
COMMENT
COLUMN_FORMAT
FIXED
DYNAMIC
DEFAULT
STORAGE
DISK
MEMORY
)

tokens.select { |t| !t.empty? }.sort.uniq.each do |t|
  puts "%s: %s;" % [t, t.split(//).map { |c| c == "_" ? "'_'" : c }.join(' ')]
end