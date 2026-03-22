grammar ICSS;

//--- LEXER: ---

// IF support:
IF: 'if';
ELSE: 'else';
BOX_BRACKET_OPEN: '[';
BOX_BRACKET_CLOSE: ']';


//Literals
TRUE: 'TRUE';
FALSE: 'FALSE';
PIXELSIZE: [0-9]+ 'px';
PERCENTAGE: [0-9]+ '%';
SCALAR: [0-9]+;


//Color value takes precedence over id idents
COLOR: '#' [0-9a-f] [0-9a-f] [0-9a-f] [0-9a-f] [0-9a-f] [0-9a-f];

//Specific identifiers for id's and css classes
ID_IDENT: '#' [a-z0-9\-]+;
CLASS_IDENT: '.' [a-z0-9\-]+;

//General identifiers
LOWER_IDENT: [a-z] [a-z0-9\-]*;
CAPITAL_IDENT: [A-Z] [A-Za-z0-9_]*;

//All whitespace is skipped
WS: [ \t\r\n]+ -> skip;

//
OPEN_BRACE: '{';
CLOSE_BRACE: '}';
SEMICOLON: ';';
COLON: ':';
PLUS: '+';
MIN: '-';
MUL: '*';
ASSIGNMENT_OPERATOR: ':=';


//--- PARSER: --
stylesheet: ( variableAssignment | styleRule )* EOF;

styleRule: selector OPEN_BRACE ruleBody CLOSE_BRACE;

// Body of a stylesheet block: declarations, nested if, or variable assignments.
ruleBody: ( declaration | ifClause | variableAssignment )*;

declaration: propertyName COLON expression SEMICOLON;
propertyName: LOWER_IDENT;
selector: classSelector | idSelector | tagSelector;
classSelector: CLASS_IDENT;
idSelector: ID_IDENT;
tagSelector: LOWER_IDENT;

variableAssignment: variableReference ASSIGNMENT_OPERATOR expression SEMICOLON;
variableReference: CAPITAL_IDENT;

// if [ guard ] { ... } [ else { ... } ]
ifClause: IF BOX_BRACKET_OPEN ifGuard BOX_BRACKET_CLOSE OPEN_BRACE ruleBody CLOSE_BRACE elseClause?;

// Separate rule so the listener can bind the condition without ambiguity.
ifGuard: expression;
elseClause: ELSE OPEN_BRACE ruleBody CLOSE_BRACE;

// --- Expressions (bottom-up: primary -> multiply -> add/sub, left-associative) ---

expression: additiveExpression;
additiveExpression: multiplicativeExpression ( ( PLUS | MIN ) multiplicativeExpression )*;
multiplicativeExpression: primaryExpression ( MUL primaryExpression )*;
primaryExpression: literal | variableReference;
literal : TRUE | FALSE | COLOR | PIXELSIZE | PERCENTAGE | SCALAR;
