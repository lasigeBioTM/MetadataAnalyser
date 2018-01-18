# MetadataAnalyser

A tool that measures metadata quality. It assesses the quality of metadata by considering the proportion of terms actually linked to ontology concepts, as well as the
specificity of the terms used in the metadata. Metadata Analyser’s frontend is available at http://masterweb-metadataanalyser.rhcloud.com.

The tool is composed of the following components (in separate folders): 
- frontend: interacts with the user by requesting a metadata file;
- engine: analyses the metadata file and evaluates the annotations found therein;
- webapi: connects the interface to the application component;

## References: 

- J. Ferreira, B. Inácio, R. Salek, and F. Couto, “Assessing public metabolomics metadata, towards improving quality,” Journal of Integrative Bioinformatics, vol. 14, no. 4, pp. 1--28, 2017 (https://doi.org/10.1515/jib-2017-0054)

- B. Inácio, J. Ferreira, and F. Couto, “Metadata analyser: measuring metadata quality,” in Practical Applications of Computational Biology and Bioinformatics (PACBB), pp. 197--204, 2017 (https://doi.org/10.1007/978-3-319-60816-7_24)
