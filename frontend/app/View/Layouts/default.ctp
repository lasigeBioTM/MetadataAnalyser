<?php
/**
 * CakePHP(tm) : Rapid Development Framework (http://cakephp.org)
 * Copyright (c) Cake Software Foundation, Inc. (http://cakefoundation.org)
 *
 * Licensed under The MIT License
 * For full copyright and license information, please see the LICENSE.txt
 * Redistributions of files must retain the above copyright notice.
 *
 * @copyright     Copyright (c) Cake Software Foundation, Inc. (http://cakefoundation.org)
 * @link          http://cakephp.org CakePHP(tm) Project
 * @package       app.View.Layouts
 * @since         CakePHP(tm) v 0.10.0.1076
 * @license       http://www.opensource.org/licenses/mit-license.php MIT License
 */

$cakeDescription = __d('cake_dev', 'CakePHP: the rapid development php framework');
$cakeVersion = __d('cake_dev', 'CakePHP %s', Configure::version())
?>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml" lang="pt" xml:lang="pt">
	<head>
		<?php echo $this -> Html -> charset('ISO-8859-1'); ?>
		<title><?php echo $title_for_layout; ?></title>
		<meta name="viewport" content="width=device-width, initial-scale=1.0">
		<?php 
			echo $this -> Html -> meta('icon');
			echo $this -> Html -> css('style');
			echo $this -> Html -> css('style-form');			
		?>
		<link href='http://fonts.googleapis.com/css?family=Raleway:400,200,300,600,700,800' rel='stylesheet' type='text/css'>
		<link href='http://fonts.googleapis.com/css?family=Open+Sans:400,700,800,600,300,200' rel='stylesheet' type='text/css'>
		<link rel="stylesheet" href="//code.jquery.com/ui/1.11.4/themes/smoothness/jquery-ui.css">		
		<?php
		echo $this -> Html -> css('font-awesome.min');
		echo $this -> Html -> css('responsive');
		echo $this -> Html -> css('jquery.sidr.dark');
		echo $this -> Html -> script('jquery.min.js');
		echo $this -> Html -> script('jquery.sidr.min.js');
		echo $this -> Html -> script('smoothscroll.js');
		echo $this -> Html -> script('metadataparser.js');		
		echo $this -> Html -> script('jsonconverter.js');
		echo $this -> fetch('meta');
		echo $this -> fetch('css');
		echo $this -> fetch('script');
	?>
	<script src="//code.jquery.com/ui/1.11.4/jquery-ui.js"></script>
	</head>
	<body>
		<div class="header">
			<div class="container">
				<div class="logo-menu">
					<div class="logo">
						<h1><a href="#">Metadata Analyser</a></h1>
					</div>
					<!--<a id="simple-menu" href="#sidr">Toggle menu</a>-->
					<div id="mobile-header">
						<a class="responsive-menu-button" href="#"><img src="images/11.png"/></a>
					</div>
					<div class="menu" id="navigation">
						<ul>
							<li><a href="#banner">Home</a></li>
							<li><a href="#metaparser">Analyser</a></li>
							<li><a href="#features">Features</a></li>
							<li><a href="#about">About</a></li>
							<li><a href="#contact">Contact</a></li>
						</ul>
					</div>
				</div>
			</div>
		</div>
		<?php echo $this->fetch('content'); ?>
		<div class="footer">
        	<div class="container">
            	<div class="infooter">
                	<p class="copyright">Copyright &copy; Extant 2016 by 
                	<a class="credit"href="http://www.html5layouts.com">HTML5 Layouts</a> | 
                	<a href="http://vectortoons.com/">Get Vector Graphics</a></p>
            	</div>
	            <ul class="socialmedia">
	                <li><a href=""><i class="icon-twitter"></i></a></li>
	                <li><a href=""><i class="icon-facebook"></i></a></li>
	                <li><a href=""><i class="icon-dribbble"></i></a></li>
	                <li><a href=""><i class="icon-linkedin"></i></a></li>
	                <li><a href=""><i class="icon-instagram"></i></a></li>
	            </ul>
            </div>
        </div>
<?php
	echo $this -> Html -> script('jquery.nicescroll.min.js',array('inline' => true));
	echo $this -> Html -> scriptBlock(
		"$(document).ready(function() {
				$('#simple-menu').sidr({
				side: 'right'
			});
			});
			$('.responsive-menu-button').sidr({
				name: 'sidr-main',
				source: '#navigation',
				side: 'right'

				});
			$(document).ready(
			function() {
			$('html').niceScroll({cursorborder:'0px solid #fff',cursorwidth:'5px',scrollspeed:'70'});
			}
			);
		",array('inline' => true)); 
	?>
	</body>
</html>
