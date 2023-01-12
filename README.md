# CRSFinder

Automatically identify and locate configuration read sites in software source code via static analysis. This tool is developed based on [ORPLocator](https://github.com/zhendong2050/ORPLocator), with much higher accuracy on inferring configuration option names and other improvements.

CRSFinder reads software source code in XML format and outputs all configuration read sites(CRS) found in it, along with their corresponding configuration option names. This tool is specially useful for advanced researches on software security related to software configuration. For example, we can check whether configuration option names in software source code and those in documentation are consistent. And we can further use the CRS as the starting points for taint analysis.

Currently, CRSFinder supports Java programs that manage their configuration options with specialized configuration classes and get-methods.

## Usage

1. Use [srcML](https://www.srcml.org/) to convert software source code to XML format. It is recommended to archive the source code first, and then generate XML file with `--position` option, i.e., `srcml --position src.zip -o output.xml`.
2. Modify src/test/Launcher.java and run it. The given Launcher.java file is for analyzing Apache Hadoop source code.
