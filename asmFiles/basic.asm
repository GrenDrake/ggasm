; include the glulx header file
include "glk.asm"

; set the stack size to 2048 bytes; this must be a multiple of 256
stackSize 2048

; declare a function for setting up the GLK environment; the zero indicates 
; that this takes no arguments
function setup 0
    setiosys ioSysGlk 0
	; _glk is a "shortcut" opcode that will automatically push the required 
	; paramaters in the appropriate order before calling the actual glk opcode
    _glk glkWindowOpen 0 0 0 wintypeTextBuffer 0 sp
	; in this case there are no paramaters so we're just using the actual glk
	; opcode
    glk glkSetWindow 1 0
	; all done; return
    return 0

; this is a basic function to demonstrate calling functions and using local 
; variables. This particular function has five locals; when called with 
; arguments, the initial value of the locals will be set to the value of the 
; arguments. If there is no corrisponding argument, then it will be set to zero.
;
; what this function actually does is take four arguments, print out the value 
; of each one, then return the result of multiplying them all together
function testFunc 5
	; start by copying a one into the fifth local; since this defaults to zero,
	; multiplying anything into it would result in zero as well
    copy 1 #4
	; print local one and multiply it into local five
    streamnum #0
    streamchar 32
    mul #4 #0 #4
	; print local two and multiply it into local five
    streamnum #1
    streamchar 32
    mul #4 #1 #4
	; print local three and multiply it into local five
    streamnum #2
    streamchar 32
    mul #4 #2 #4
	; print local four and multiply it into local five
    streamnum #3
    streamchar 10
    mul #4 #3 #4
	; return the final result
    return #4

; add a string to the string table and give it the specified label; the string 
; table will be located in ROM space and will combine duplicate strings
addString theBigString "Hello world! I am a really good string!\n"

; define a string outside the string table; note that you can use any characters
; in the string, but the file needs to be UTF-8 encoded. also, string will be 
; automatically set to unicode or basic depending on their contents - this will
; be a unicode string because it contains unicode character
string testUnicodeString "Hello 常用漢字\n"
; create and label a four byte space; this is the required size to hold a memory
; address
bytesFixed strloc            4

; define the main function; every program must have a main function and this is
; where execution begins
function main 1
	; call the GLK setup function; this is required for us to have any output
    call setup 0 0

	; verify the game file and put the result on the stack
    verify sp
	; if the top of the stack is a zero, the verify was successful and we can 
	; jump over printing the error message
    jeq sp 0 endverify
	; print an error message; the inline string will be added to the string 
	; table and replaced with the address of the string
    streamstr "Verify failed.\n";
	; the label for the previous jump; note that an instruction can follow a 
	; label on the same line, even if it doesn't in this case
endverify:

	; copy the address of "testUnicodeString" into the space at the address of
	; strloc - the * indicates the opcode should act on the contents of the 
	; address rather than the address itself
    copy testUnicodeString *strloc
	; print the string at the address "theBigString"
    streamstr theBigString
    
	; call the test function defined earlier; put the results on the stack
    _call testFunc 8234 3224 67 2 sp
	; output the result of the test function
    streamstr "std testFunc: " 
    streamnum sp
    streamchar 10
	
	; output the string located at the address contained at the address strloc
    streamstr *strloc
    streamchar 10
	
	; all done; return
    return 0
