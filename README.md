# MetadataAnalyser

A tool that measures metadata quality. It assesses the quality of metadata by considering the proportion of terms actually linked to ontology concepts, as well as the
specificity of the terms used in the metadata. Metadata Analyser’s frontend is available at http://masterweb-metadataanalyser.rhcloud.com.

The tool is composed of the following components (in separate folders): 
- frontend: interacts with the user by requesting a metadata file;
- engine: analyses the metadata file and evaluates the annotations found therein;
- webapi: connects the interface to the application component;
