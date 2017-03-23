<?php
/**
 * @link          http://cakephp.org CakePHP(tm) Project
 * @package       app.View.Pages
 * @since         CakePHP(tm) v 0.10.0.1076
 */

	if (!Configure::read('debug')):
		throw new NotFoundException();
	endif;
	
	App::uses('Debugger', 'Utility');
	
	$this->assign('title', 'Metadata Analyser');
?>
	<div id="banner" class="banner">
    	<div class="container">
        	<div class="header-text">
            	<p class="big-text">Let's Start Metadata Analysis</p>
                <h2>Improving Metadata Usage</h2>
                <p class="small-text">Metadata Analyser aims at rating metadata integration on the semantic web.</p>
                <div class="button-section">
                	<ul>
                    	<li><a href="#about" alt="Learn more about Metadata Analyser" 
                    	class="top-button white">Learn More</a></li>
                        <li><a href="#metaparser" alt="Start metadata analysis process" 
                        class="top-button green">Get Started</a></li>
                    </ul>
                </div>
            </div>
        </div>
    </div>
    <div class="color-border"></div>
    <div id="metaparser" name="metaparser" class="desc">
    	<div class="container">
        	<h2>Metadata Specificity and Coverage Analysis</h2>
			<p>Please, fill out the following form fields and push Start Metadata Analysis button.</p>
			<p>After pressing the submit button, the metadata file file or location analysis process starts. It 
			will take just a few seconds for the result to be showed. For each result a new tab is created and 
			appended to the existing ones.</p>
			<?php echo $this -> Html -> script('metadataparser.js',array('inline' => false)); ?>
			<br />
			<div id="formcontainer" name="formcontainer">
            	<form enctype="multipart/form-data" method="post">
					<table class="form-container">
						<tr>
							<td class="field-desc">
								<label for="file-source">Metadata Repository:</label>
							</td>
							<td class="field-value">
								<label>MetaboLights - European Bioinformatics Institute</label>
							</td>
						</tr>						
						<tr>
							<td class="field-desc">
								<label for="file">Metadata File:</label>
							</td>
							<td class="field-value">
								<input type="file" placeholder="Please choose a metadata file" name="metafile" 
								size="45" accept=".txt" />
							</td>
						</tr>
						<tr>
							<td class="field-desc">
								<label for="file">Metadata Location:</label>
							</td>
							<td class="field-value">
								<input type="text" id="metaurl"  name="metaurl" placeholder="Set the URL for a metadata file" 
								value="ftp://ftp.ebi.ac.uk/pub/databases/metabolights/studies/public/MTBLS1/i_Investigation.txt"/>
							</td>
						</tr>							
					</table>
				</form>
				<button class="submit" type="submit" id="submitbutton" name="submitbutton">Start Metadata Analysis</button>
				<div id="progresslayer" name="progresslayer">
					<div id="progressbar">
						<div class="progress-label">Metadata file analysis is about to start...</div>
					</div>
					<div id="progresspan" name="progressspan"></div>		
				</div>										
        	</div>
        	<br />
        	<div id="ajaxlayer" name="ajaxlayer"></div>
        </div>
    </div>
    <div class="features" id="features">
    	<div class="container">
        	<h3 class="text-head">Features Of Metadata Analyser</h3>
        	<div class="features-section">
                <ul>
                	<li>
                    	<div class="feature-icon icon1"></div>
                     	<h4>Efficient Interface</h4>
                        <p>Using a simple user interface design, taking usability as paramount, 
                        Metadata Analyser Web Site tries to focus its users to the main objective: 
                        metadata analysis</p>
                     </li>
                     <li>
                    	<div class="feature-icon icon2"></div>
                     	<h4>Reduced Waiting Times</h4>
                        <p>This web interface is part of an architecture design aimed at decreasing user 
                        waiting times, making computing parallelism one of its corner stones</p>
                     </li>
                     <li>
                    	<div class="feature-icon icon3"></div>
                     	<h4>No Customization</h4>
                        <p>There's no need for any user customization, regarding the parsing process. Just 
                        an actual metadata file, or online repository file location, is needed to start the 
                        process.</p>
                     </li>
                </ul>
            </div>
        </div>
    </div>
    <div style="clear:both;"></div>
    <div id="about" name="about" class="desc">
    	<div class="container">
        	<h2>About Metadata Analyser</h2>
			<p class="align">&quot;Metadata Analyser&quot; aims at metadata quality measurement, regarding the specificity and coverage values 
			from metadata semantic integration with a standardized public vocabulary, in order to filter out ambiguity and subjectivity in used terms. 
			This semantic integration is quantified and qualified trough a parsing and measurement process that collects 
			metadata description terms and evaluates (1) all URI annotated terms, regarding its position in the referenced Ontology, (2) the 
			ratio between annotated and non-annotated terms.</p>
			<p class="align">At the moment only Metobolights repository public metadata files are supported in this case study version and only four metadata 
			classes were considered meaningful in this process: Design, Factor, Assay and Protocol. For each class two values are calculated: average specificity 
			and coverage. Both values only take into account class&#039;s terms.</p>
			<h3>Solution Architecture</h3>
			<p class="align" style="margin-top: 0px;">This web interface is part of a architectured solution that comprises three major tiers: data, logical 
			and presentation tier. At the heart of the data tier is a relational data repository. All of the necessary Metolobolights ontologies 
			were previously downloaded and converted into a relational model and persisted into this database. This process has been made available 
			by a convertion solution (<a href="https://github.com/jotomicron/OWLtoSQL">OWLtoSQL</a>), developed by Jo&atilde;o Ferreira (FCUL), that uses 
			<a href="http://owlapi.sourceforge.net/">OWL API</a> to extract information from an OWL ontology to a SQL back-end.</p>
			<p class="align">The logical tier comprises two layers: (1) endpoint layer, (2) parsing and measurement server layer. The endpoint layer comprises a set of resources, made available by 
			a RESTful web service endpoint (using Java JAX-RS), which enables the interaction between presentation layer and parsing and measurement server components. The 
			server component, for a given metadata file, tries to get a measurement value from the ontologies repository for each annotation found and 
			calculate the ratio between annotated and non-annotated existing terms.
			<p class="align">The presentation layer offers the user a web interface, implemented over <a href="http://cakephp.org/">CakePHP</a> development framework, 
			capable of upload a metafile to be analysed, in the simplest manner.</p>
			<h3>Interface Results</h3>
			<p class="align">After a metadata file or metadata file location is given, the analysis process takes an average of one minute to show the result. The result is appended 
			to an existing one. For each result the user can read the specificity values for each metadata annotation found, an average specificity value for each 
			metadata class processed and an overall average study specificity value. If the parsing and measurement server is incapable of determine the specificity annotation 
			value, a non-available (NA) remark is used instead. The coverage value, i.e., the ratio between annotated and non-annotated terms is displayed for each class and 
			for each study, in an overall manner.</p>
			<p class="align">For each study is also displayed the study identification, the process job unique identification, the parsing and measurement elapsed time, a list of 
			referenced Ontologies by URI and a list of used metadata classes. Each result can be downloaded in JSON or CSV formats.</p>
			<h3>Thesis Development</h3>
			<p class="align"><i>Metadata Analyser</i> is a Master degree thesis development contribution solution, in the field of Computer Science from the Science Faculty 
			of the Lisbon University (<a href="https://ciencias.ulisboa.pt/en">FCUL</a>), entitled &quot;How much is Metadata Worth&quot;. The thesis objective is to develop a tool that allows knowledge 
			level measurement from a metadata data set, regarding its specificity and completeness web semantic integration. The value found will be used as foundation of a new 
			rating, recognizing and rewarding mechanism, whose main element is a virtual currency.</p>
			<br>
			<p>Thesis development team:</p>
			<br>
			<p>Bruno In&aacute;cio</p>
			<p>Student</p>
			<br>
			<p>Jo&atilde;o Almeida</p>
			<p>Coordinator Assistant</p>
			<br>
			<p>Francisco M. Couto</p>
			<p>Coordinator</p>		
        </div>
    </div>    
    <div class="contact" id="contact">
    	<div class="container">
        	<h3 class="text-head">Contact </h3>
            <p class="box-desc">For any other Metadata Analyser project questions, please send a message to:</p>
            <br />
            <p class="box-desc" style="line-height: 30px;">Bruno In&aacute;cio</p>
            <p class="box-desc" style="line-height: 30px;">FCUL Student</p>
            <p class="box-desc" style="line-height: 30px;">fc40846@alunos.fc.ul.pt</p>
        </div>
    </div>
    <div class="color-border">        
