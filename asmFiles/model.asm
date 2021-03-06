include "glk.asm"

; model.c in GGASM ported by Gren Drake; original copyright statement
; and file description are preserved below. This port is made available under
; the same terms.
;
; One slight difference between this version and the original model.c is that
; I have used a search/jump table for handling the player's commands rather 
; than a list of string comparisons. This is as much to demonstrate the ability
; to do this as it is for any other reason.

; model.c: Model program for Glk API, version 0.5.
;  Designed by Andrew Plotkin <erkyrath@eblong.com>
;  http://www.eblong.com/zarf/glk/index.html
;  This program is in the public domain.

; This is a simple model of a text adventure which uses the Glk API.
;  It shows how to input a line of text, display results, maintain a
;  status window, write to a transcript file, and so on.

; This is the cleanest possible form of a Glk program. It includes only
;  "glk.h", and doesn't call any functions outside Glk at all. We even
;  define our own str_eq() and str_len(), rather than relying on the
;  standard libraries.

stackSize 2048

; The story, status, and quote windows.
bytesFixed mainwin      4
bytesFixed statuswin    4
bytesFixed quotewin     4
; A file reference for the transcript file.
bytesFixed scriptref    4
; A stream for the transcript file, when it's open.
bytesFixed scriptstr    4
; Your location. This determines what appears in the status line.
; We're using four bytes for what is essentially a boolean because the glulx
; VM works with 4 byte words
bytesFixed current_room 4
; A flag indicating whether you should look around.
bytesFixed need_look    4
; a buffer for storing player commands in
bytesFixed commandbuf   256

basicString roomOneName "The Room"
basicString roomTwoName "A Different Room"

bytesFixed event        16
; The glk_main() function is called by the Glk system; it's the main entry
; point for your program.
function main 3
        ; set the io system to GLK
        setiosys ioSysGlk 0
        ; Open the main window.
        _glk glkWindowOpen 0 0 0 wintypeTextBuffer 1 *mainwin
        jne *mainwin 0 windowOpened
        ; It's possible that the main window failed to open. There's
        ; nothing we can do without it, so exit.
        quit;

    windowOpened:
        ; Set the current output stream to print to it.
        _glk glkSetWindow *mainwin 0

        ; Open a second window: a text grid, above the main window, three lines
        ; high. It is possible that this will fail also, but we accept that.
        copy winmethodAbove #0
        bitor #0 winmethodFixed #0
        _glk glkWindowOpen *mainwin #0 3 wintypeTextGrid 0 *statuswin

        ; The third window, quotewin, isn't opened immediately. We'll do
        ; that in verb_quote().

        streamstr "Model Glk Program\nAn Interactive Model Glk Program\n
                   By Andrew Plotkin.\nRelease 7.\n
                   Ported to GGASM Assembly by Gren Drake\n
                   Type \"help\" for a list of commands.\n"

        copy 0 *current_room ; set initial location.
        copy 1 *need_look



    mainLoopBegin:
        _call drawStatusWindow 0

        jne *need_look 1 skipLook
        streamchar '\n'
        _glk glkSetStyle styleSubheader 0
        jne *current_room 0 otherRoom
        streamstr roomOneName
        streamchar 10
        jump doneName
    otherRoom:
        streamstr roomTwoName
        streamchar 10
    doneName:
        _glk glkSetStyle styleNormal 0
        streamstr "You're in a room of some sort.\n"
    skipLook:

        streamstr "\n> ";
        ; We request up to 255 characters. The buffer can hold 256, but we
        ; are going to stick a null character at the end, so we have to
        ; leave room for that. Note that the Glk library does *not*
        ; put on that null character.
        _glk glkRequestLineEvent *mainwin commandbuf 255 0  0

    inputLoop:
        _glk glkSelect event 0

        jeq *event evtypeArrange   evWindowArrange
        jeq *event evtypeLineInput evLineInput
        streamstr "Received unexpected GLK event"
        jump inputLoop

    evWindowArrange:
        jump inputLoop
    evLineInput:
;       Really the event can *only* be from mainwin,
;       because we never request line input from the
;       status window. But we do a paranoia test,
;       because commandbuf is only filled if the line
;       event comes from the mainwin request. If the
;       line event comes from anywhere else, we ignore
;       it.
        aload event 1 #0
        jne #0 *mainwin inputLoop
        jump gotLine


    gotLine:
;       commandbuf now contains a line of input from the main window.
;       You would now run your parser and do something with it.

;       First, if there's a blockquote window open, let's close it.
;       This ensures that quotes remain visible for exactly one
;       command.
        jeq *quotewin 0 skipCloseQuoteWindow
        _glk glkWindowClose *quotewin 0 0
        copy 0 *quotewin
    skipCloseQuoteWindow:

;       The line we have received in commandbuf is not null-terminated.
;       We handle that first.
        aload event 2 #0
        astoreb commandbuf #0 0

;       Then squash to lower-case. (this does not use the newer unicode aware
;       functions... oh well)
        copy 0 #0
    inputCaseLoop:
        aloadb commandbuf #0 #1
        jeq #1 0 endCaseLoop
        _glk glkCharToLower #1 #1
        astoreb commandbuf #0 #1
        add #0 1 #0
        jump inputCaseLoop
    endCaseLoop:

;       Then trim whitespace before and after.
        aload event 2 #0
        sub #0 1 #0
    trimTrailingLoop:
        aloadb commandbuf #0 #1
        jne #1 32 doneTrimTrailing
        astoreb commandbuf #0 0
        sub #0 1 #0
        ; if we've hit the beginning of the string, we're done
        jlt #0 0 doneTrimTrailing
        jump trimTrailingLoop
    doneTrimTrailing:

        ; to trim the initial whitespace we first need to find the first
        ; non-space character
        copy 0 #0
    findOffsetLoop:
        aloadb commandbuf #0 #1
        jne #1 32 doneTrimInitialFindOffset
        jeq #1 0  doneTrimInitialFindOffset
        add #0 1 #0
        jump findOffsetLoop

        ; then we copy the characters after that point to their respective
        ; places starting at the beginning of the buffer
    doneTrimInitialFindOffset:
        copy 0 #1
    trimInitialLoop:
        aloadb commandbuf #0 #2
        astoreb commandbuf #1 #2
        jeq #2 0 doneTrimInitial
        add #0 1 #0
        add #1 1 #1
        jump trimInitialLoop
    doneTrimInitial:

        linearsearch commandbuf VOCAB_WORD_SIZE vocabTable VOCAB_SIZE -1 0 3 #0
        jeq #0 0 unknownCommand
        aload #0 3 #0
        _call #0 0
        jump mainLoopBegin
        
    unknownCommand:
        streamstr "I don't understand the command \""
        _call printBuffer commandbuf 0
        streamstr "\".\n"
        jump mainLoopBegin

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
; the verb table
constant VOCAB_SIZE      16
constant VOCAB_WORD_SIZE 12
vocabTable:
words _ $68656C70 $0        $0 verbHelp
words _ $6A756D70 $0        $0 verbJump
words _ $6D6F7665 $0        $0 verbMove
words _ $71756974 $0        $0 verbQuit
words _ $71756F74 $65000000 $0 verbQuote
words _ $72657374 $6F726500 $0 verbRestore
words _ $73617665 $0        $0 verbSave
words _ $73637269 $0        $0 verbScript
words _ $756E7363 $72697074 $0 verbUnscript
words _ $79616461 $0        $0 verbYada
words _ $0        $0        $0 0 ; end of table

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
; the verb functions
function verbHelp 0
    streamstr "This model only understands the following commands:\n
               HELP: Display this list.\n
               JUMP: A verb which just prints some text.\n
               YADA: A verb which prints a very long stream of text.\n
               MOVE: A verb which prints some text, and also changes the status line display.\n
               QUOTE: A verb which displays a block quote in a temporary third window.\n
               SCRIPT: Turn on transcripting, so that output will be echoed to a text file.\n
               UNSCRIPT: Turn off transcripting.\n
               SAVE: Write fake data to a save file.\n
               RESTORE: Read it back in.\n
               QUIT: Quit and exit.\n"
    return 0


function verbJump 0
        streamstr "You jump on the fruit, spotlessly.\n"
        return 0


function verbMove 0
        copy 1 *need_look
        jeq *current_room 0 verbMoveBack
        copy 0 *current_room
        return 0
    verbMoveBack:
        copy 1 *current_room
        return 0


function verbSave 3
        copy fileusageSavedGame #0
        bitor #0 fileusageBinaryMode #0
        _glk glkFilerefCreateByPrompt #0 filemodeWrite 0 #0
        jne #0 0 vsRefGood
        streamstr "Unable to place save file.\n"

    vsRefGood:
        _glk glkStreamOpenFile #0 filemodeWrite 0 #1
        _glk glkFilerefDestroy #0 0
        jne #1 0 vsStrGood
        streamstr "Unable to write to save file.\n"
        return 0

    vsStrGood:
        copy 0 #2
    vsLoop:
        _glk glkPutCharStream #1 #2 0
        add #2 1 #2
        jne #2 256 vsLoop

        _glk glkStreamClose #1 0 0
        streamstr "Game saved.\n"
        return 0

function verbRestore 4
        copy fileusageSavedGame #0
        bitor #0 fileusageBinaryMode #0
        _glk glkFilerefCreateByPrompt #0 filemodeRead 0 #0
        jne #0 0 vrRefGood
        streamstr "Unable to find save file.\n"

    vrRefGood:
        _glk glkStreamOpenFile #0 filemodeRead 0 #1
        _glk glkFilerefDestroy #0 0
        jne #1 0 vrStrGood
        streamstr "Unable to read from save file.\n"
        return 0

    vrStrGood:
        copy 0 #2
    vrLoop:
        _glk glkGetCharStream #1 #0

        ; check for EOF
        jne #0 $ffffffff vrLoopGood
        streamstr "Unexpected end of file.\n"
        copy 1 #3
        jump vrDone

    vrLoopGood:
         ; check appropriate character
        jeq #0 #2 vrNoError
        streamstr "This does not appear to be a valid saved game.\n"
        copy 1 #3
        jump vrDone

    vrNoError:
        add #2 1 #2
        jlt #2 256 vrLoop


    vrDone:
        _glk glkStreamClose #1 0 0
        jeq #3 0 vrDoneSuccess
        streamstr "Failed.\n"
        return 0

    vrDoneSuccess:
        streamstr "Game restored.\n"
        return 0


function verbScript 1
        jeq *scriptstr 0 vsNotAlreadyOn
        streamstr "Scripting is already on.\n"
        return 0

;       If we've turned on scripting before, use the same file reference;
;       otherwise, prompt the player for a file.
    vsNotAlreadyOn:
        jeq scriptref 0 vsHaveScriptRef
        copy fileusageTranscript #0
        bitor #0 fileusageTextMode #0
        _glk glkFilerefCreateByPrompt #0 filemodeWriteAppend 0 *scriptref
        jne *scriptref 0 vsHaveScriptRef
        streamstr "Unable to place script file.\n"
        return 0

    vsHaveScriptRef:
;       Open the file.
        _glk glkStreamOpenFile *scriptref filemodeWriteAppend 0 *scriptstr
        jne *scriptstr 0 vsScriptGood
        streamstr "Unable to write to script file.\n"
        return 0

    vsScriptGood:
        streamstr "Scripting on.\n"
        _glk glkWindowSetEchoStream *mainwin *scriptstr 0
        _glk glkPutStringStream *scriptstr "This is the beginning of a transcript.\n" 0
        return 0

function verbQuit 0
        streamstr "Are you sure you want to quit? "
        _call yesOrNo sp
        jne sp 1 vqNotReally
        streamstr "Thanks for playing.\n"
        quit;
;       glk_exit() actually stops the process; it does not return.
    vqNotReally:
        return 0


function verbQuote 1
        streamstr "Someone quotes some poetry.\n"

;       Open a third window, or clear it if it's already open. Actually,
;       since quotewin is closed right after line input, we know it
;       can't be open. But better safe, etc.
        jeq *quotewin 0 verbQuoteOpen
        _glk glkWindowClear *quotewin 0
        jump verbQuotePrint

    verbQuoteOpen:
;       A five-line window above the main window, fixed size.
        copy winmethodAbove #0
        bitor #0 winmethodFixed #0
        _glk glkWindowOpen *mainwin #0 3 wintypeTextBuffer 0 *quotewin
        jne *quotewin 0 verbQuotePrint
;       It's possible the quotewin couldn't be opened. In that
;       case, just give up.
        return 0

    verbQuotePrint:
;       Print some quote.
        _glk glkSetWindow *quotewin 0
        _glk glkSetStyle styleBlockQuote 0
        streamstr "Tomorrow probably never rose or set\n
                   Or went out and bought cheese, or anything like that\n
                   And anyway, what light through yonder quote box breaks\n
                   Handle to my hand?\n"
        streamstr "              -- Fred\n"
        _glk glkSetWindow *mainwin 0
        return 0


function verbUnscript 0
        jne *scriptstr 0 vuDoit
        streamstr "Scripting is already off.\n"
        return 0

    vuDoit:
;       Close the file.
        _glk glkPutStringStream *scriptstr "This is the end of a transcript.\n\n" 0
        _glk glkStreamClose *scriptstr 0 0
        streamstr "Scripting off.\n"
        copy 0 *scriptstr
        return 0

function verbYada 0
        streamstr "This has not been implemented yet.\n"
        return 0

; static void verb_yada(void)
; {
;     /* This is a goofy (and overly ornate) way to print a long paragraph.
;         It just shows off line wrapping in the Glk implementation. */
;     #define NUMWORDS (13)
;     static char *wordcaplist[NUMWORDS] = {
;         "Ga", "Bo", "Wa", "Mu", "Bi", "Fo", "Za", "Mo", "Ra", "Po",
;             "Ha", "Ni", "Na"
;     };
;     static char *wordlist[NUMWORDS] = {
;         "figgle", "wob", "shim", "fleb", "moobosh", "fonk", "wabble",
;             "gazoon", "ting", "floo", "zonk", "loof", "lob",
;     };
;     static int wcount1 = 0;
;     static int wcount2 = 0;
;     static int wstep = 1;
;     static int jx = 0;
;     int ix;
;     int first = TRUE;
;
;     for (ix=0; ix<85; ix++) {
;         if (ix > 0) {
;             glk_put_string(" ");
;         }
;
;         if (first) {
;             glk_put_string(wordcaplist[(ix / 17) % NUMWORDS]);
;             first = FALSE;
;         }
;
;         glk_put_string(wordlist[jx]);
;         jx = (jx + wstep) % NUMWORDS;
;         wcount1++;
;         if (wcount1 >= NUMWORDS) {
;             wcount1 = 0;
;             wstep++;
;             wcount2++;
;             if (wcount2 >= NUMWORDS-2) {
;                 wcount2 = 0;
;                 wstep = 1;
;             }
;         }
;
;         if ((ix % 17) == 16) {
;             glk_put_string(".");
;             first = TRUE;
;         }
;     }
;
;     glk_put_char('\n');
; }


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
; some basic system functions

; printBuffer buffer
; prints a string contained in a buffer (i.e. not a glulx string)
function printBuffer 3
        copy 0 #1
    printBufferLoop:
        aloadb #0 #1 #2
        jeq #2 0 printBufferDone
        streamchar #2
        add #1 1 #1
        jump printBufferLoop
    printBufferDone:
        return 0

; stringCompare buffer1 buffer2
; compare two strings. returns < 0 if the first goes earlier than the second,
; > 0 if it goes after, or 0 if they are the same
function stringCompare 5
        copy 0 #2
    compareLoop:
        aloadb #0 #2 #3
        aloadb #1 #2 #4
        jlt #3 #4 compareStringLT
        jgt #3 #4 compareStringGT
        jeq #3 0  compareStringEqual
        add #2 1 #2
        jump compareLoop
    compareStringLT:
        return -1
    compareStringGT:
        return 1
    compareStringEqual:
        return 0

; stringLength buffer
; determine the length of a string in bytes
function stringLength 3
        copy 0 #1
    slLoop:
        aloadb #0 #1 sp
        jeq sp 0 slFoundLength
        add #1 1 #1
        jump slLoop
    slFoundLength:
        return #1

bytesFixed statusWinSize  8
; drawStatusWindow
; draws the content of the status window
function drawStatusWindow 2
        jne *statuswin 0 goodStatusWindow
;       It is possible that the window was not successfully
;       created. If that's the case, don't try to draw it.
        return 0

    goodStatusWindow:
        _glk glkSetWindow *statuswin 0
        _glk glkWindowClear *statuswin 0
        _glk glkWindowGetSize *statuswin statusWinSize 0 0

;       Get the current room name
        jne *current_room 0 dswRoomTwo
        copy roomOneName #1
        jump dswRoomDone
    dswRoomTwo:
        copy roomTwoName #1
    dswRoomDone:

;       Print the room name, centered.
        copy *statusWinSize #0
        _call stringLength #1 sp
        sub #0 sp #0
        div #0 2 #0
        _glk glkWindowMoveCursor *statuswin #0 1 0
        streamstr #1

;       Draw a decorative compass rose in the upper right.
        copy *statusWinSize #0
        sub #0 3 #0
        _glk glkWindowMoveCursor *statuswin #0 0 0
        streamstr "\\|/"
        _glk glkWindowMoveCursor *statuswin #0 1 0
        streamstr "-*-"
        _glk glkWindowMoveCursor *statuswin #0 2 0
        streamstr "/|\\ " ; the extra space is because of a bug in the lexer

;       all done; set the main window as current again then return
        _glk glkSetWindow *mainwin 0
        return 0

; yesOrNo
; get a yes or no answer from the player. Print the prompt of your choice
; before calling this. Will return 1 for yes or 0 for no
function yesOrNo 2
    _call drawStatusWindow 0

;       This loop is identical to the main command loop in glk_main().
    yonRestartInputLoop:
        _glk glkRequestLineEvent *mainwin commandbuf 255 0  0
    yonInputLoop:
        _glk glkSelect event 0
        jeq *event evtypeArrange   yonWindowArrange
        jeq *event evtypeLineInput yonLineInput
        streamstr "Received unexpected GLK event"
        jump yonInputLoop

    yonWindowArrange:
        jump yonInputLoop
    yonLineInput:
        aload event 1 #0
        jne #0 *mainwin yonInputLoop
        jump yonGotLine

    yonGotLine:
        aload event 2 #0
        astoreb commandbuf #0 0
        copy 0 #0
    yonLoop:
        aloadb commandbuf #0 #1
        jeq #1 0 yonBad
        jne #1 32 yonAnswer
        add #1 1 #1
        jump yonLoop

    yonAnswer:
        jeq #1 'Y' yonYes
        jeq #1 'y' yonYes
        jeq #1 'N' yonNo
        jeq #1 'n' yonNo
    yonBad:
        streamstr "Please enter \"yes\" or \"no\": "
        jump yonRestartInputLoop
    yonYes:
        return 1
    yonNo:
        return 0
