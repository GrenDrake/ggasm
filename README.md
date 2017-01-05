# GGASM

This is a basic assembler for the Glulx virtual machine. It's still a work in progress, but should be nearly usable in it's current state.

To build an assembly file using GGASM, two arguments are required: the name of the input file and the name of the output file.

More information on Glulx and Glk is available from Andrew Plotkin's specifications on each, both accessible from [his Gulx page](http://www.eblong.com/zarf/glulx/).

## Status

In its current version, GGASM is fully capable of transforming source files into functional Glulx game files.

Floating point support is currently experimental and needs further testing.

There are a few notable features that have not yet been implemented yet:

* Labels and constants cannot currently be used in data statements
* Character constants

## Building

To build GGASM, you should use Gradle; the Gradle wrapper is included (either gradlew or gradlew.bat depending on platform).

## Source Files

A source file is made up of three kinds of statements: Directives, Labels, and Instructions.

Strings included through the addString directive or directly as instruction operands will be added to the string table. This table will be put into the ROM area of the game file and thus cannot be modified. Currently strings are set to basic or Unicode as appropriate, but in future versions will be Huffman encoded instead.

### Directives

Directives must always occur on their own line. Typically, a directive instructions the assembler to do something rather than being directly assembled into output. 

**addString \<label-name\> "string text"** Add a string to the string table and give it the specified label.

**bytes \<label-name\> [\<byte value\>+]** Add a sequence of raw bytes into the game file at the current location and create the specified label. Each byte must be a valid 8-bit value.

**bytes \<label-name\> \<field-length\> [\<byte value\>+]** As with bytes above, but will always insert the specified number of bytes, padding with zeros as necessary and issuing an error if more than *field-length* bytes are specified.

**constant \<name\> \<value\>** Creates a named constant with the specified value. The value must be numeric.

**function \<label-name\> \<local-count\>** Defines the start of a function with the specified name and the specified number of local variables.

**include "filename"** This will include the specified file into the assembled output at the location of the directive.

**includeBinary \<label-name\> \<size-name\> "filename"** Include a raw binary file directly in the Glulx game file. A label will be created with the specified name for the starting address of the data and a constant will be created with the size of the added data.

**stackSize \<size\>** This will direct the assembler to create a Glulx file specifying the stated size for the stack. The stack size must always be a multiple of 256.

**stkfunction \<label-name\> \<local-count\>** Create a function (as per *function* above), but put arguments passed on the stack rather than into the local variables. The argument count will also be pushed onto the stack.

**string \<label-name\> "string text"** Include a string at the current point in the file; unlike strings stored in the string table, this string can be altered by the game during play. Currently the assembler will automatically choose between using a basic or Unicode string, but later versions will allow the type to the specified.

**toROM** and **endROM** Everything between these two directives will be added to the game file's ROM area rather than to the main memory area.

### Labels

A label is created by specifying the name of the label followed by a colon:

```
labelName:
```

A label may be followed by an instruction (but not a directive) on the same line:

```
labelName: copy 1 #0
```

### Instructions

An instruction consists of a mnemonic followed by a series of operands. For the meaning of the various mnemonics, check [Andrew Plotkin's glulx spec](http://www.eblong.com/zarf/glulx/glulx-spec.html).

An operand may be one of an integer or floating point number, a constant, a label, or a string. In addition, an integer may be preceded by a pound sign (#) to indicate a local variable; #0 is the first local variable, #1 the second, etc. An operand may also be preceded by a asterisk to indicate the opcode should use the contents of the memory location specified. So, _\*labelName_ would use what was contained at labelName rather than labelName itself; see the opcode list in the spec linked above to get an idea where this should be used.

There are two "shortcut" opcodes as well: \_glk and \_call. This will be expanded by the assembler into the required copy operations to put the arguments onto the stack in the correct order before calling the indicated opcode.
