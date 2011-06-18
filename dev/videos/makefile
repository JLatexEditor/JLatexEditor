include config.make

all: config.make first_vid.in

config.make:
	@echo "#######################################"
	@echo "# Please configure config.make first! #"
	@echo "#######################################"
	@echo "TutoMakeDir=..." >config.make

%.in:
	wget http://common.jle.cc/dev/videos/$*.in.diff.tar.bz2
	tar xfj $*.in.diff.tar.bz2
	$(TutoMakeDir)/tutomake -decompress $*.in.diff.tar.bz2
