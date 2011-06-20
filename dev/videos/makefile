include config.make

videos = first_vid.in

all: check_config $(videos)

check_config: config.make $(TutoMakeDir)/tutomake

config.make:
	@echo "TutoMakeDir=..." >config.make

$(TutoMakeDir)/tutomake:
	@echo "#######################################"
	@echo "# Please configure config.make first! #"
	@echo "#######################################"
	@exit 1

%.in: %.in.diff.tar.bz2
	tar xfj $*.in.diff.tar.bz2
	$(TutoMakeDir)/tutomake -decompress $*.in.diff.tar.bz2

%.in.diff.tar.bz2:
	wget http://common.jle.cc/dev/videos/$*.in.diff.tar.bz2
