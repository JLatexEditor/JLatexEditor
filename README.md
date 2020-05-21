
![Logo](/assets/img/logo.png)

# JLatexEditor


JLatexEditor is a cross-platform open source LaTeX editor. It creates a unique development environment for a user by integrating many tools needed to write LaTeX documents, and is highly configurable.

The editor is in constant development. It is well-tested on Linux and Mac OS X, but has to be considered pre-alpha version, and only for experimental use for Windows.

![JLatex editor showing LaTeX compiler error in the editor](/assets/screenshot/screenshot_0.2.10_showing_latex_error_mini.png) ![JLatex editor showing a list of bibtex entries in the completion for `\cite{}`](/assets/screenshot/screenshot_0.1.28_cite_completion_minor_restricted_mini.png)


## Operating System Support

* GNU/Linux
* Mac OS
* Windows: highly experimental (not yet for production use)

The support for Windows will be improved in the next stable release.

## Requirements

 - **Oracle Java** (recommended JDK 8 or higher)
 - **Apache Ant** 

## Installation:

### Usage
Download the lastest release for your platform, unpack it. Run the script `jlatexeditor` (or `jlatexeditor.bat` in case of Windows) to start the editor.


### Include the Ubuntu/Debian Repository
Including the Debian/Ubuntu repository of `apt.endrullis.de` allows you to install and update the JLatexEditor via `apt-get` or `aptitude`.

#### Ubuntu Users 
Add the repository:
```bash
# create sources list
sudo echo 'deb http://apt.endrullis.de/ natty main' >/etc/apt/sources.list.d/endrullis-natty.list
# import gpg key packages are signed with
sudo wget http://apt.endrullis.de/public.gpg -O- | sudo apt-key add -
```
Install the JLatexEditor:
```bash
sudo aptitude update
sudo aptitude install jlatexeditor
```

#### Debian Users
<a name="debrepo"></a>

Add the repository (run as root):
```bash
# create sources list
echo 'deb http://apt.endrullis.de/ sid main' >/etc/apt/sources.list.d/endrullis-sid.list
# import gpg key packages are signed with
wget http://apt.endrullis.de/public.gpg -O- | sudo apt-key add -
```
Install the JLatexEditor:
```bash
aptitude update
aptitude install jlatexeditor
```

### Latest release

[See](CHANGELOG) what's new. Users can also download the latest release, pre-packaged for most popular systems:
+ Linux: [​tar.gz](http://endrullis.de/JLatexEditor/releases/JLatexEditor-latest.tar.gz),
    - Debian / Ubuntu: [Using repository](#debrepo), [deb Package](http://endrullis.de/JLatexEditor/releases/jlatexeditor-latest.deb)
    - Arch Linux: [Package](https://web.archive.org/web/20150806063924/https://aur.archlinux.org/packages.php?ID=44123) (created by Matthias Busl) 
+ Mac: [tar.gz](http://endrullis.de/JLatexEditor/releases/JLatexEditor-latest.tar.gz)
+ Windows: [zip](http://endrullis.de/JLatexEditor/releases/JLatexEditor-latest.zip)



### Development

Clone the repository, build and run:

```bash
git clone https://github.com/JLatexEditor/JLatexEditor.git`
cd JLatexEditor
./start.sh
```

If you would like to run JLatexEditor in the debug mode, please use `startDebug.sh`
instead of `start.sh`.

## Documentation

* [Manual](https://github.com/JLatexEditor/JLatexEditor/wiki)
* for question and issues, use the issue tracker of this repository.
* [Contribution guidelines for this project](docs/CONTRIBUTING.md)

For the list of current features, see [Features wiki page](Features).


## Credits

Developers that took part in the development of JLatexEditor:

* [Jörg Endrullis](http://joerg.endrullis.de/)
* [Stefan Endrullis](http://stefan.endrullis.de/)
* [Rena Bakhshi](http://www.few.vu.nl/~rbakhshi/)

## License

This program is free software; you can redistribute it and/or modify it under the ​GNU General Public License version 3. See the [LICENSE](LICENSE) file for more details.
