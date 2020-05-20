
![Logo](/assets/img/logo.png)

# JLatexEditor



JLatexEditor is a cross-platform open source LaTeX editor. The editor is in constant development. It is well-tested on Linux and Mac OS X, but has to be considered pre-alpha version, and only for experimental use for Windows.

## Operating System Support

* GNU/Linux
* Mac OS
* Windows: highly experimental (not yet for production use)

The support for Windows will be improved in the next stable release.

## Install requirements
TODO: update this

* Java (recommended JDK 8 or higher)
* Apache Ant
* Maven 

## Download

### Installation 
Download the lastest release for your platform, unpack it and run the script `jlatexeditor` (or `jlatexeditor.bat` in case of Windows) to start the editor

### Latest release
+ Linux: [​tar.gz](http://endrullis.de/JLatexEditor/releases/JLatexEditor-latest.tar.gz),
    - Debian / Ubuntu: [deb Package](http://endrullis.de/JLatexEditor/releases/jlatexeditor-latest.deb)
    - Arch Linux: [Package](https://web.archive.org/web/20150806063924/https://aur.archlinux.org/packages.php?ID=44123) (created by Matthias Busl) 
+ Mac: [tar.gz](http://endrullis.de/JLatexEditor/releases/JLatexEditor-latest.tar.gz)
+ Windows: [zip](http://endrullis.de/JLatexEditor/releases/JLatexEditor-latest.zip)

[See](CHANGELOG) what's new? 


The ​older releases ...
 

## Documentation

* Manual (wiki link)
* for post questions please use the mailing list ​jle-users 

## Screenshots

![JLatex editor showing LaTeX compiler error in the editor](/assets/screenshot/screenshot_0.2.10_showing_latex_error_mini.png)

![JLatex editor showing a list of bibtex entries in the completion for `\cite{}`](/assets/screenshot/screenshot_0.1.28_cite_completion_minor_restricted_mini.png)

## Features

* syntax highlighting:
    + red marking of undefined labels in `\ref`, non-existing .bib entries in \cite, and non-existing files in \input, ...
    + yellow marking of unused labels in \label
    + fast bracket identification (including highlighting of nested brackets)
    + user defined highlighting of latex commands and their parameters 
* live spell checker (aspell and hunspell integration)
* multi-level completion of
    + LaTeX commands, environments, packages, and documentclasses (level 1 shows most frequently applied items, level 2 also the less frequent ones, and level 3 just all)
    + directories and files after \input{, \include{, and \includegraphics{ (level 1 shows only suitable file types, level 2 all files) 
* completion of
    + bibtex entries after \cite{
    + labels after \ref{
    + referenced labels after \label{
    + package names after \usepackage{
    + environment names after \begin{ and \end{
    + document class names after \documentclass{
    + bibliography styles after \bibliographystyle{
    + tikz packages after \usetikzlibrary{
    + beamer themes after \usetheme{, \usecolortheme{, \usefonttheme{, \useinnertheme{, \useoutertheme{
    + words which have already been used in the document 
* refactoring
    + command extraction: allows you to create a macro from existing LaTeX code
    + renaming of commands, labels, environments, and bibtex entries
    + realignment of table columns 
* forward search and inverse search
* command templates and user defined live templates
* quick help for LaTeX commands
* LaTeX compiler integration
    + separate lists for errors, warnings, overfilled hboxes
    + jump to code position with one click
    + advanced latex output parser (we usually point to the right error location) 
* diff integration (compare two files)
* version control
    + svn integration
    + local version history (see what you have changed in the last minutes, hours, and days) 
* show document structure and allow quick jump to sections
* quick jump to element under cursor (file, label, command, environment, bibtex entry)
* fast file creation: just press alt+enter over an non-exsisting file after an \input{, \include{, and \includegraphics{ command
* shortcut for closing current environment
* new environments are closed automatically (if not yet done)
* automatic indentation when starting a new block (opening bracket, opening environment, continuing item text)
* support for scripting (Haskell) within latex documents (svn version)
* tree magic helps to create beautiful tikz trees within seconds (svn version) 

![Tree feature](/assets/img/tree.png)

* features by screenshots 

## In work

* multi-language support (partially available) 

* for more features and fixed coming soon see the list of ​open tickets for milestone 0.3 

## How to get involved?

There are several way you can contribute to this project:

* report bugs
* suggest enhancements or new features
* translate JLatexEditor into your language
* take part in the development (send a request to our mailing list ​jle-dev or contact ​Stefan Endrullis / ​Jörg Endrullis)
* to keep track of the development or to discuss bugs and features join the mailing list ​jle-dev 

## Source code

Clone the repository:

`git clone https://github.com/JLatexEditor/JLatexEditor.git`

Build and run:

`ant runJLatexEditor`

Development:

(TODO broken link to wiki)

## Credits

Developers that took part in the development of JLatexEditor:

* ​Jörg Endrullis
* ​Stefan Endrullis
* ​Rena Bakhshi 

## License

This program is free software; you can redistribute it and/or modify it under the ​GNU General Public License version 3. See the [LICENSE](LICENSE) file for more details.

## Links

* ​Wikipedia article about the JLatexEditor 

## JLatexEditor on Twitter

* Follow us on ​Twiter @latexeditor 
