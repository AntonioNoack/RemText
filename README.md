# RemText

Simple, fast text editor, because "TextEditor" on Ubuntu was too slow for me.
Supports auto-formatting for JSON and XML.

![Promo.png](promo/Promo.png)
*I had a 4MB single-line JSON file, and it would lag horribly for no apparent reason

Supports basic syntax highlighting for
- C, C++, Rust, Zig, Go,
- C#, Java, Kotlin, Swift,
- JavaScript, HTML, CSS, PHP,
- JSON, YAML, XML, CSV,
- Markdown, Shell,
- GLSL, HLSL.

Support multi-cursor editing 😺.

## Controls
- F1: toggle light/dark theme
- F2: toggle wrapped lines
- arrow keys: move cursor
- shift-arrow keys: extend selection
- keyboard keys: typing
- control-a: select all
- control-c: copy
- control-v: paste
- control-x: cut (copy + clear)
- control-y/z: undo
- control-shift-y/z: redo
- control-s: save
- control-f: open find/search menu
    - up/down arrow: go to next/previous found item
- control-r: open replace menu
    - up/down arrow: go to next/previous found item
    - enter: replace item / go to next one
    - tab: switch between search/replace input
- control-shift-f: autoformat (JSON only at the moment)
- mouse-drag: select text
- mouse-click: set cursor
- mouse-wheel: scroll
- page-up/down: change font size
- escape: exit search/replace; exit program
- alt-shift-click: add another cursor where you clicked
- double-control+up/down: add another cursor in the line above/below

## Planned Features
- Save-as-menu?
- New-file-menu?
- Rendering unknown symbols as hex-code

## Not-yet Planned
- Emojis
- Custom Fonts
- Custom Themes
- HexEdit-Mode

## OS-Support
This implementation uses OpenGL 3.3, but could be easily switched with Java AWT if needed.
It should be supported by Windows, Linux and MacOS equally.

## Icon
I generated a nice icon with ChatGPT :)

![Icon64.png](assets/Icon256.png)
