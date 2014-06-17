// The Main JS Document
// Author: Ulrich Omasits
// Date: 2011.01.30

var svg; // stores the jquery svg object
var svgDOM; // stores the svg dom element
var svgContainer; // stores the svg container jquery object
var lblStatus; 
var tblProteins;

var zoom = 5; // zoom change per interval in percent
var zoomInterval; // javascript-interval for zooming animation 

var aaSelectionStart = -1;
var aaSelectionEnd = -1;

var queryString; // stores the protter query url
var query = {peps:[], mods:{}};

var basePath = "";
var createPath = "create";

var useSvgAnimation = false;
var useFileReader = false;
var useFormAttr = false;
var defaultStyles = true;

var isMulti = false;
var isUniprot = true;

var proteinFilter="";

var rangeMenu;

var proteases = {
	"peptidecutter.Tryps": "Trypsin",
	"peptidecutter.LysC": "LysC",
	"peptidecutter.LysN": "LysN",
	"peptidecutter.ArgC": "Arg-C proteinase",
	"peptidecutter.AspN": "Asp-N endopeptidase",
	"peptidecutter.AspGluN": "Asp-N endopeptidase + N-terminal Glu",
	"peptidecutter.BNPS": "BNPS-Skatole",
	"peptidecutter.Casp1": "Caspase1",
	"peptidecutter.Casp2": "Caspase2",
	"peptidecutter.Casp3": "Caspase3",
	"peptidecutter.Casp4": "Caspase4",
	"peptidecutter.Casp5": "Caspase5",
	"peptidecutter.Casp6": "Caspase6",
	"peptidecutter.Casp7": "Caspase7",
	"peptidecutter.Casp8": "Caspase8",
	"peptidecutter.Casp9": "Caspase9",
	"peptidecutter.Casp10": "Caspase10",
	"peptidecutter.Ch_hi": "Chymotrypsin-high specificity",
	"peptidecutter.Ch_lo": "Chymotrypsin-low specificity",
	"peptidecutter.Clost": "Clostripain (Clostridiopeptidase B)",
	"peptidecutter.CNBr": "CNBr",
	"peptidecutter.Enter": "Enterokinase",
	"peptidecutter.Xa": "Factor Xa",
	"peptidecutter.HCOOH": "Formic acid",
	"peptidecutter.Glu": "Glutamyl endopeptidase",
	"peptidecutter.GranB": "GranzymeB",
	"peptidecutter.Hydro": "Hydroxylamine",
	"peptidecutter.Iodo": "Iodosobenzoic acid",
	"peptidecutter.NTCB": "NTCB (2-nitro-5-thiocyanobenzoic acid)",
	"peptidecutter.Pn1.3": "Pepsin (pH1.3)",
	"peptidecutter.Pn2": "Pepsin (pH>2)",
	"peptidecutter.Pro": "Proline-endopeptidase",
	"peptidecutter.ProtK": "Proteinase K",
	"peptidecutter.Staph": "Staphylococcal peptidase I",
	"peptidecutter.TEV": "Tobacco etch virus protease",
	"peptidecutter.Therm": "Thermolysin",
	"peptidecutter.Throm": "Thrombin"
};

$(document).ready(function(){
		// check for parameters
		//var params = decodeURIComponent(location.search.substring(1));
		//var get = string2associativeArray(params, "&", "=");
	if (location.href.indexOf("#")==-1) {
		window.location.replace("start/");
		return;
	}
	
	// run a couple of compatibility tests
	if (!Modernizr.svg || !Modernizr.inlinesvg || !Modernizr.svgfilters) {
		$("#body").html("redirecting to 'www.mozilla.com'");
		alert("Your browser does not support SVG properly! Please use Mozilla Firefox, Google Chrome, or a similar modern web browser.");
		//window.location = "http://www.mozilla.com";
	}
	useSvgAnimation = Modernizr.smil;
	useFileReader = Modernizr.filereader;
	useFormAttr = Modernizr.formattribute;
	
	/*
	 * setting styles and initializing jquery ui elements
	 */
	$("#divContainer").tabs({ disabled: [ 1, 2, 3 ] });
	
	$("#divMain").splitter({type: "v", sizeLeft: true});
	$(".vsplitbar").append("<span class='ui-icon ui-icon-carat-1-e' style='margin-top:-5px;margin-left:-7px;display:inline-block;position:absolute;top:50%;'></span>");
	$(".vsplitbar").append("<span class='ui-icon ui-icon-carat-1-w' style='margin-top:5px;margin-left:-7px;display:inline-block;position:absolute;top:50%;'></span>");
	
	$("#divContent").splitter({type: "h", sizeTop: true});
	$(".hsplitbar").append("<span class='ui-icon ui-icon-carat-1-n' style='margin-top:-4px;margin-right:-3px;display:inline-block'></span>");
	$(".hsplitbar").append("<span class='ui-icon ui-icon-carat-1-s' style='margin-top:-4px;margin-left:-3px;display:inline-block'></span>");
	
	$("#logo").click( function() {
		return confirm('You will loose all the settings made so far. Do you really want to return to the start page?');
	} );
	
	/*
	 * Sequence Tab
	 */
	$("#divSequenceInput").tabs();
	
	$("#frmID").submit(function(e, data) {
		e.preventDefault();
		if ($("#txtID").val().length == 0) {
			alert("Please enter a protein identifier first!");
			return false;
		}
		isUniprot = true;
		if (data===undefined) {
			if (isMulti)
				unselectProteins();
			query.peps = [];
			query.mods = {};
			$(".rangeMenuItemProteomics").hide();
			$(".rangeMenuItemProteomics", rangeMenu).hide();
		} else {
			query.peps = data.peps;
			query.mods = data.mods;
			$(".rangeMenuItemProteomics").show();
			$(".rangeMenuItemProteomics", rangeMenu).show();
		}
		refresh();
		return false;
	});
	$("#btnSubmitID").button({ icons: { primary: "ui-icon-check" } });
	$("#btnExampleID").button({ icons: { primary: "ui-icon-lightbulb" } }).click(function(){
		$("#txtID").val("BST2_HUMAN");
		$("#frmID").submit();
		return false;
	});
	
	$("#frmSequence").submit( loadProteinsFromSequences );
	$("#btnSubmitSequence").button({ icons: { primary: "ui-icon-check" } });
	$("#btnExampleSequence").button({ icons: { primary: "ui-icon-lightbulb" } }).click(function(){
		$("#txtSequence").val("MASTSYDYCRVPMEDGDKRCKLLLGIGILVLLIIVILGVPLIIFTIKANSEACRDGLRAV\nMECRNVTHLLQQELTEAQKGFQDVEAQAATCNHTVMALMASLDAEKAQGQKKVEELEGEI\nTTLNHKLQDASAEVERLRRENQVLSVRIADKKYYPSSQDSSSAAAPQLLIVLLGLSALLQ");
		$("#frmSequence").submit();
		return false;
	});
	
	$("#btnLoadFasta").button({ icons: { primary: "ui-icon-folder-open" } }).click(function() {
		$("#inpFasta").click();
		return false;
	});
	$("#inpFasta").css("visibility","hidden").change( function(event) {
		var file = this.files[0];
		$("#frmSequence")[0].reset();
		var reader = new FileReader();
		reader.onloadend = function(e) {
			if (e.target.readyState == FileReader.DONE) { // DONE == 2
				$("#txtSequence").val(e.target.result);
				$("#frmSequence").submit();
			}
		};
		reader.readAsText(file);
	});
	
	$("#btnProteinIDs").button({ icons: { primary: "ui-icon-document" } }).click(function(){
		$("#dlgProteinIDs").dialog("open");
	});
	$("#dlgProteinIDs").dialog({
		resizable: true,
		autoOpen:false,
		modal: true,
		width:600,
		height:500,
		buttons: [
			{text: 'load example', click: function() {
				$("#txtProteinIDs").val("P10747\r\nFFAR2_MOUSE\r\nQ9H6X2 DFNETQLAR,INDSVTLNEK\r\nCXCR5_MOUSE SDLSR\r\nBST2_HUMAN ASTSYDYCR,NVTHLLQQELTEAQK,VEELEGEITTLNHK\r\nITA4_HUMAN TRPVVIVDASLSHPESVNR,IGFLKPHENK,MNLTFHVINTGN\r\nS29A1_HUMAN LDMSQNVSLVTAELSK\r\nSIRPG_HUMAN GTANLSEAIR");
			}},
			{text: 'submit', type: 'submit', form: 'dlgProteinIDs', click: function() { 
				if (!useFormAttr) $(this).submit(); }
			},
			{text: 'cancel', click: function() {
				$(this).dialog('close');
			}}
		]
	});
	$("#dlgProteinIDs").submit( loadProteinsFromIdList );
	
	$("#btnProteomics").button({ icons: { primary: "ui-icon-folder-open" } }).click(function() {
		if (useFileReader) {
			$("#dlgProteomics").dialog("open");
		} else {
			alert("Sorry, your browser does not support local file processing.\nPlease use a more recent version of your favorite browser (IE v10, Safari v6) to use this Protter feature.");
		}
	});
	$("#dlgProteomics").dialog({
		resizable: true,
		autoOpen: false,
		modal: true,
		width:450,
		height:450,
		buttons: [
			{text: 'submit', type: 'submit', form: 'dlgProteomics', click: function() { 
				if (!useFormAttr) $(this).submit(); }
			},
			{text: 'cancel', click: function() {
				$(this).dialog('close');
			}}
		]
	});
	$("#dlgProteomics").submit( loadProteinsFromProteomics );
	$("#inpProteomics").change( function(event) {
		$("#dlgProteomics").submit();
	});
	
	// load rangeMenu contents once
	/*$.ajax({ url: "rangeMenu.html", 
			 success: function(result) { rangeMenu = $(result); },
			 async: false
	});*/
	rangeMenu = $('#rangeMenu');
	
	/*
	 * sidebar - protein table
	 */
	tblProteins = $("#tblProteins");
	tblProteins.treetable({ expandable: true, clickableNodeNames: true });
	
	$("#proteinFilter").keyup( function() { $("#proteinFilter").change() } );
	$("#proteinFilter").change( function() {
		if ($("#proteinFilter").val() != proteinFilter) {
			proteinFilter = $("#proteinFilter").val();
			var filterPattern = new RegExp(proteinFilter, "i");
			$("tr", tblProteins).each( function (index, elem) {
				var row = $(elem);
				if (! row.hasClass("peptideRow")) {
					var text = row.children().first().text().trim();
					if (filterPattern.test(text)) {
						console.log(text);
						row.show();
					} else {
						var nodeId = row.attr("data-tt-id");
						tblProteins.treetable("collapseNode", nodeId);
						row.hide();
					}
				}
			});
		}
	});
	
	$("#btnReset").button({ icons: { primary: "ui-icon-close" } }).click( reset );
	
	$("#btnExportAll").button({ icons: { primary: "ui-icon-disk" } }).click( function() {
		$("#dlgExportAll").dialog('open');
		return false;
	});
	$("#dlgExportAll").dialog({
		resizable: false,
		autoOpen:false,
		modal: true,
		width: 500,
		height: 300,
		buttons: [
			{text: 'submit', type: 'submit', form: 'dlgExportAll', click: function() { 
				if (!useFormAttr) $(this).submit(); }
			},
			{text: 'cancel', click: function() {
				$(this).dialog('close');
			}}
		]
	});
	$("#dlgExportAll").submit( function() {
		if (! /^\S+@\S+$/.test($("#email").val())) {
			alert("Please specify a valid email address first!");
			return false;
		}
		//submit ids, peptides and mods for batch visualization
		var format = $("#batchFormat").val();
		updateQueryString(format);
		$.ajax({
			url: "batch",
			async: true,
			data: queryString+"&email="+escape($("#email").val())+"&proteins="+escape(tblProteins.data("batch")),
			type: "POST"
		}).done(function(msg) { 
			if (msg.indexOf("Error:")==0)
				showMessageAlert("Error", msg.substring(6), 5);
			else
				showMessageInfo("Success", msg, 5); 
		}).fail(function(response) {
			showMessageAlert("Error", response.responseText, 5); 
		});
		updateQueryString("svg");
		$("#dlgExportAll").dialog('close');
		
		return false;
	});
	
	$("#btnUniprot").button({ icons: { primary: "ui-icon-newwin" } }).click(function() {
		if (isUniprot) {
			if (query.up.toLowerCase().indexOf("sp|") == 0 || query.up.toLowerCase().indexOf("tr|") == 0)
				window.open("http://www.uniprot.org/uniprot/"+query.up.split("|")[1]);
			else
				window.open("http://www.uniprot.org/uniprot/"+query.up);
		} else {
			window.open("http://www.uniprot.org/blast/?query="+query.seq);
		}
	});
	
	
	$("#btnZoomOut").button({ icons:{primary:"ui-icon-zoomout"}, text: false }).mousedown(function() {
		zoomOut();
		zoomInterval = window.setInterval(zoomOut, 50);
	});
	$("#btnZoomIn").button({ icons:{primary:"ui-icon-zoomin"}, text: false }).mousedown(function() {
		zoomIn();
		zoomInterval = window.setInterval(zoomIn, 50);
	});
	$("#btnFitToScreen").button({ icons: { primary: "ui-icon-arrow-4-diag"}, text: false }).click(function() {
		svgFitToScreen();
	});
	
	$("#btnSave").button({ icons: { primary: "ui-icon-disk" } }).click(function(){
		updateQueryString();
		$("#saveAsMenu a").each(function() {
			// updateQueryString(this.title);
			this.href = createPath + "?" + queryString + "&format=" + this.title;
		});
		//updateQueryString("svg");
	});
	$("#btnSave").menu({ content: 
		"<ul id='saveAsMenu'>" +
			"<li><a href='#' title='png' target='_blank'>Raster Graphic (png)</a></li>" +
			"<li><a href='#' title='svg' target='_blank'>Vector Graphic (svg)</a></li>" +
			"<li><a href='#' title='pdf' target='_blank'>Vector Graphic (pdf)</a></li>" +
			"<li><a href='#' title='pptx' target='_blank'>PowerPoint (pptx)</a></li>" +
		"</ul>"
		, flyOut: true });
		
	$("#btnReload").button({ icons: { primary: "ui-icon-refresh" } }).click(function() {
		refresh();
	});
	$("#btnMail").button({ icons: { primary: "ui-icon-mail-closed" } }).click(function() {
		updateQueryString("svg");
		var body = "Hello,\nI want to share with you a Protter protein plot:\n"+location+"\n\n";
		location.href="mailto:?subject=Protter&body="+encodeURIComponent(body);
	});
	
	$("#btnNewStyle").button({ icons:{primary:"ui-icon-plus"}, text: false }).click(function() {
		defaultStyles=false;
		addStyle("bc:blue","");
	});
	$("#btnClearStyles").button({ icons:{primary:"ui-icon-trash"}, text: false }).click(function() {
		if (confirm('Do you really want to remove all the style definitions?')) {
			clearStyles();
			refresh();
		}
	});
	
	$("#divShapeWrapper").buttonset();
	$("div.color_picker").hover(
		function(){ $(this).addClass("ui-state-hover"); }, 
		function(){ $(this).removeClass("ui-state-hover"); }
	);
	
	$("#tblStylesBody").tableDnD({
		dragHandle: "dragHandle",
		onDrop: refresh
	});
	
	$("#txtTransmembrane").uoRangeInput("<li><a href='#' class='rangeMenuItemSelectedRange' title='' style='display:none'>selected range</a></li><li><a href='#' title='UP.TRANSMEM'>UniProt trans-membrane regions</a></li><li><a href='#' title='Phobius.TM'>Phobius trans-membrane regions</a></li>");
	$("#txtIntramembrane").uoRangeInput("<li><a href='#' class='rangeMenuItemSelectedRange' title='' style='display:none'>selected range</a></li><li><a href='#' title='UP.INTRAMEM'>UniProt intra-membrane regions</a></li>");
	$("#txtAnchors").uoRangeInput("<li><a href='#' class='rangeMenuItemSelectedRange' title='' style='display:none'>selected range</a></li><li><a href='#' title='UP.LIPID'>UniProt Lipidation</a></li>");
	
	// making textboxes tab aware
	$('textarea.tabEnabled').keydown(function(e) {
		if(e.keyCode == 9) {
			var start = $(this).get(0).selectionStart;
			$(this).val($(this).val().substring(0, start) + "\t" + $(this).val().substring($(this).get(0).selectionEnd));
			$(this).get(0).selectionStart = $(this).get(0).selectionEnd = start + 1;
			return false;
		}
	});
	
	/*
	 * the status span
	 */
	lblStatus = $("#status");
	
	/*
	 * adding the controls
	 */
	$("body").mouseup(function() {
		window.clearInterval(zoomInterval);
	});
	$("body").mouseover(function() {
		lblStatus.text("");
	});
	
	/*
	 * initialize the svg
	 */
	svgContainer = $("#divSvgContainer");
	svg = svgContainer.svg().svg("get");
	if (!svg) {
		$("#body").html("redirecting to 'www.mozilla.com'");
		alert("Your browser does not support SVG! Please use Mozilla Firefox or a similar modern web browser.");
		window.location = "http://www.mozilla.com";
	}
	
	// manually re-layout on resize
	$("#divContent").resize(function() {
		resizeStyleTable();
		$("#tabMain").css("padding-left",$("#divContent").position().left+30);
	});
	
	// initializations after loading settings	
	$(".colorPicker").colorPicker();
	
	$("#divTMlabelWrapper").buttonset();
	
	$("#divNtermWrapper").buttonset().change(function() {
		$("#divNtermWrapper label").removeClass("ui-corner-left ui-corner-right");
		$("#divNtermWrapper label").first().addClass("ui-corner-top");
		$("#divNtermWrapper label").last().addClass("ui-corner-bottom");
	});
	$("#divNtermWrapper label[for='selNtermPhobius']").click(function() {
		$("#txtTransmembrane").val("PHOBIUS.TM");
		$("#txtIntramembrane").val("");
		$("#txtAnchors").val("");
	});
	$("#divNtermWrapper label[for='selNtermUniprot']").click(function() {
		$("#txtTransmembrane").val("UP.TRANSMEM");
		$("#txtIntramembrane").val("UP.INTRAMEM");
		$("#txtAnchors").val("UP.LIPID");
	});
	$("#divNtermWrapper").buttonset().change();
	
	
	$("#divTopoWrapper").buttonset().change(function() {
		if ($("#divTopoWrapper input:checked").val() == "none" || $("#divTopoWrapper input:checked").val() == "auto") {
			$("#divNtermWrapper").buttonset("disable");
			$("#txtTransmembrane").attr("disabled", "disabled");
			$("#txtIntramembrane").attr("disabled", "disabled");
			$("#txtAnchors").attr("disabled", "disabled");
		} else {
			$("#divNtermWrapper").buttonset("enable");
			$("#txtTransmembrane").removeAttr("disabled");
			$("#txtIntramembrane").removeAttr("disabled");
			$("#txtAnchors").removeAttr("disabled");
		}
		$("#divTopoWrapper label").removeClass("ui-corner-left ui-corner-right");
		$("#divTopoWrapper label").first().addClass("ui-corner-top");
		$("#divTopoWrapper label").last().addClass("ui-corner-bottom");
	});
	$("#divTopoWrapper label[for='selTopoCustom']").click(function() {
		var nterm = $("#divNtermWrapper input:checked").val();
		if (!nterm) {
			$("#divNtermWrapper input[value='intra']").prop("checked",true).button("refresh");
		}
	});
	$("#divTopoWrapper").buttonset().change();
	
	loadProteases();
	
	$("#bolNumbers").button();
	$("#bolLegend").button();
	
	// add change-listeners to auto-update UI
	$("#divTopoWrapper input").change(refresh);
	$("#divNtermWrapper input").change(refresh);
	$("#txtTransmembrane").change(refresh);
	$("#txtIntramembrane").change(refresh);
	$("#txtAnchors").change(refresh);
	
	$("#colMembrane").change(refresh);
	$("#colTMlabel").change(refresh);
	$("#divTMlabelWrapper input").change(refresh);
	$("#lstProtease").change(refresh);
	$("#bolNumbers").change(refresh);
	$("#bolLegend").change(refresh);
	$("#txtTextopo").change(refresh);	
	
	$("#divUniprotDialog").dialog({
		resizable: true,
		autoOpen:false,
		modal: true,
		width:600,
		height:230,
		buttons: {
			'Submit': function() {
				$("#txtID").val($('#lstResults').val()); // search with selected up id
				$("#divUniprotDialog").dialog('close');
				$("#frmID").submit();
			},
			'Cancel': function() {
				$("#divUniprotDialog").dialog('close');
			}
		}
	});
	
	// load settings or apply defaults
	loadFromQueryString();
	
	var params = decodeURIComponent(location.hash.substring(1));
	var queryElements = string2associativeArray(params, "&", "=");
	if (queryElements["file"]) {
		loadProteinsFromProteomics([queryElements["file"]]); // as array -> loading file from server
	} else {
		if (location.hash!="" && location.hash!="#")
			refresh();
	}

	// open the curtains!
	$("#divContainer").css("visibility", "visible");
	$("#body").removeClass("loading");
	$("#body").addClass("ui-widget-header");
	
	$("#message").click( function() { $("#message").hide(); } );
	//showMessageAlert("Problems with UniProt", "The current version of UniProt comes with a broken interface and therefore Protter currently cannot query UniProt resources. The UniProtJAPI team is working on a solution.");
});

/*
 * generate protter query string
 */

function refresh() {
	console.log("refreshing");
	if (isUniprot) {
		$("#divSequenceInput").tabs("option", "active", 0);
	} else {
		$("#divSequenceInput").tabs("option", "active", 1);
	}
	svgAAunselect();
	reloadRangeMenues();
	
	if (updateQueryString("svg")) {
		//location.hash = basePath + "?" + queryString;
		$("#divContainer").tabs("option", "disabled", []);
		location.hash = queryString;
		loadSVG();
	} else {
		queryString = "";
		loadSVG();
	}
}
function updateQueryString(format){
	console.log("update query string");
	var queryElements = new Array();
	
	if (format==undefined || format.indexOf("batch")!=0) { // for batch queryString, no up,seq,peptides,mods
		if (isUniprot) {
			//query.up = $("#txtID").val();
			if ($("#txtID").val().length==0) {
				alert("Please enter a protein identifier first!");
				return false;
			}
			queryElements.push("up=" + $("#txtID").val());
			if (query.peps.length > 0) {
				queryElements.push("peptides=" + query.peps.join(","))
			}
			if (Object.keys(query.mods).length > 0) {
				for(var i=0; i<Object.keys(query.mods).length; i++) {
					var strMod = Object.keys(query.mods)[i];
					var strPeps = Object.keys(query.mods[strMod]);
					strMod = generateModString(strMod);
					queryElements.push(strMod + "=" + strPeps.join(","))
				}
			}
		} else {
			var seq = $("#txtSequence").val().trim().replace(/^>.*/g,"").replace(/\s/g,"");
			queryElements.push("seq=" + seq);
			if (seq.length==0) {
				alert("Please enter an amino acid sequence first!");
				return false;
			}
		}
	}
	
	var topo = $("#divTopoWrapper input:checked").val();
	if (topo=="auto") {
		query.tm = "auto";
		queryElements.push("tm=auto");
	} else if (topo=="custom") {
		var nterm = $("#divNtermWrapper input:checked").val();
		if (!nterm) {
			alert("Please specify the location of the N-terminus in the 'Topology' section first.");
			return false;
		} else if (nterm.length > 0) {
			query.nterm = nterm;
			queryElements.push("nterm=" + nterm);
			var tmRegions = $("#txtTransmembrane").val().replace(/\s+/g,",")
			if (tmRegions.length>0) {
				query.tm = tmRegions;
				queryElements.push("tm=" + tmRegions);
			}
			var imRegions = $("#txtIntramembrane").val().replace(/\s+/g,",")
			if (imRegions.length>0) {
				query.im = imRegions;
				queryElements.push("im=" + imRegions);
			}
			var anchors = $("#txtAnchors").val().replace(/\s+/g,",");
			if (anchors.length>0) {
				query.anchor = anchors;
				queryElements.push("anchor=" + anchors);
			}
		}
	} else if (topo=="none") {
		// no membrane, no parameters
	}
	
	var membraneColor = $("#colMembrane").val();
	query.mc = membraneColor;
	queryElements.push("mc="+membraneColor);
	var tmLabelColor = $("#colTMlabel").val();
	query.lc = tmLabelColor;
	queryElements.push("lc="+tmLabelColor);
	var tmLabel = $("#divTMlabelWrapper input:checked").val();
	query.tml = tmLabel;
	queryElements.push("tml="+tmLabel);
	query.numbers = $("#bolNumbers").prop('checked');
	if (query.numbers)
		queryElements.push("numbers");
	query.legend = $("#bolLegend").prop('checked');
	if (query.legend)
		queryElements.push("legend");
	
	var protease = $("#lstProtease").val();
	if (protease.length>0) {
		query.protease = protease;
		queryElements.push("cutAt=" + protease);
	}
	
	var textopo = $("#txtTextopo").val().replace(/\s+/g,";");
	if (textopo.length > 0) {
		queryElements.push("tex="+textopo);
	}
	
	//if (defaultStyles==true && getStyles().length==0) {
	if (defaultStyles==true) {
		clearStyles();
		if (isUniprot) {
			//different default styles for proteomics / peptides input
			if (query.peps.length > 0)
				addStyle("n:exp.peps,fc:darkblue","EX.PEPTIDES");
			addStyle("n:signal peptide,fc:red,bc:red","UP.SIGNAL");
			addStyle("n:disulfide bonds,s:box,fc:greenyellow,bc:greenyellow","UP.DISULFID");
			addStyle("n:variants,s:diamond,fc:orange,bc:orange","UP.VARIANT");
			addStyle("n:PTMs,s:box,fc:forestgreen,bc:forestgreen","UP.CARBOHYD,UP.MOD_RES");
			if (query.peps.length > 0)
				addStyle("n:exp.peps,cc:white,bc:blue","EX.PEPTIDES");
			if (Object.keys(query.mods).length > 0)
				addStyle("n:exp.mods,fc:darkblue,bc:forestgreen","EX.MODALL");
		} else { // sequences
			addStyle("n:signal peptide,fc:red,cc:white,bc:red","Phobius.SP");
			addStyle("n:N-glyco motif,s:box,fc:forestgreen,bc:forestgreen","(N).[ST]");
		}
		//defaultStyles=false;
	}
	
	$("#tblStylesBody tr").each(function() {
		var styleElements = new Array();
		var active = $("div.btnActive", this).hasClass("ui-state-active");
		if ( ! active)
			styleElements.push("inactive");
		var name = $("input[name='name']", this).val();
		if (name.length>0)
			styleElements.push("n:"+name);
		var shape = $("input.selShape:checked", this).val();
		if (shape.length>0)
			styleElements.push("s:"+shape);
		var charColor = $("input.txtStyleColorText", this).val();
		if (charColor.length>0)
			styleElements.push("cc:"+charColor);
		var frameColor = $("input.txtStyleColorFrame", this).val();
		if (frameColor.length>0)
			styleElements.push("fc:"+frameColor);
		var backgroundColor = $("input.txtStyleColorBackground", this).val();
		if (backgroundColor.length>0)
			styleElements.push("bc:"+backgroundColor);
		var range = $("textarea.txtStyleRange", this).val().replace(/\s+/g,",");
		if (styleElements.length > 0)
			queryElements.push(styleElements.join(",") + "=" + range);
	});
	
	if (format) {
		query.format = format;
		queryElements.push("format="+format);
	}
	
	queryString = queryElements.join("&");
	
	return true;
}
/*
 * load settings from url-hash or set defaults
 */
function loadFromQueryString(){
	console.log("loading from query string");
	
	var params = decodeURIComponent(location.hash.substring(1));
	var queryElements = string2associativeArray(params, "&", "=");
	
	query = {peps:[], mods:{}};
	
	isMulti = false;
	$("#divSequenceSelection").hide();
	$("div.vsplitbar").hide();
	$("#divContent").data("previousLeft",$("#divContent").css("left"));
	$("#divContent").css("left","");
	$("#divContent").resize();
	
	if (queryElements["seq"]) {
		$("#txtSequence").val(queryElements["seq"]);
		$("#txtID").val("");
		//query.seq = queryElements["seq"];
		isUniprot = false;
	} else if (queryElements["up"]) {
		$("#txtID").val(queryElements["up"]);
		$("#txtSequence").val("");
		//query.up = queryElements["up"];
		isUniprot = true;
	} else { // no sequence given --> setting global webApp defaults
		queryElements["tm"] = "auto";
		queryElements["numbers"] = null;
		queryElements["legend"] = null;
		$("#txtSequence").val("");
		$("#txtID").val("");
		//query.up = "";
		isUniprot = true;
	}
	
	var nterm = queryElements["nterm"] ? queryElements["nterm"] : "";
	var tmRegions = queryElements["tm"] ? queryElements["tm"] : "";
	var imRegions = queryElements["im"] ? queryElements["im"] : "";
	if (tmRegions.toLowerCase()=="auto") {
		$("#divTopoWrapper input[value='auto']").prop('checked',true).button("refresh");
		$("#divTopoWrapper").buttonset().change();
	} else {
		$("#txtTransmembrane").val(tmRegions.replace(/,/g,"\n"));
		$("#txtIntramembrane").val(imRegions.replace(/,/g,"\n"));
		if (nterm=="") { // NONE
			$("#divTopoWrapper input[value='none']").prop('checked',true).button("refresh");
			$("#divTopoWrapper").buttonset().change();
		} else { // CUSTOM
			$("#divNtermWrapper input[value='"+nterm+"']").prop('checked',true).button("refresh");
			$("#divNtermWrapper").buttonset().change();
			$("#divTopoWrapper input[value='custom']").prop('checked',true).button("refresh");
			$("#divTopoWrapper").buttonset().change();
		}
	}
	
	var anchors = queryElements["anchor"] ? queryElements["anchor"] : "";
	$("#txtAnchors").val(anchors.replace(/,/g,"\n"));
	
	var membraneColor = queryElements["mc"] ? queryElements["mc"] : "lightsalmon";
	$("#colMembrane").val(membraneColor);
	$("#colMembrane").trigger("refresh");
	
	var tmLabelColor = queryElements["lc"] ? queryElements["lc"] : "blue";
	$("#colTMlabel").val(tmLabelColor);
	$("#colTMlabel").trigger("refresh");
	
	var tmLabel = queryElements["tml"] ? queryElements["tml"] : "numcount";
	$("#divTMlabelWrapper input[value='"+tmLabel+"']").prop("checked", true).button("refresh");
	
	var protease = queryElements["cutAt"] ? queryElements["cutAt"] : "";
	$("#lstProtease").val(protease);
	
	$("#bolNumbers").prop('checked', ("numbers" in queryElements)).button("refresh");
	
	$("#bolLegend").prop('checked', ("legend" in queryElements)).button("refresh");
	
	var textopo = queryElements["tex"] ? queryElements["tex"] : "";
	$("#txtTextopo").val(textopo.replace(/;/g,"\n"));
	
	clearStyles();
	for (var key in queryElements) {
		if (key.indexOf(":") != -1) { // is a style (styleDefinitions:ranges)
			defaultStyles = false;
			addStyle(key, queryElements[key]);
			//TODO: handle empty styles properly!!!
		}
	}
}


/*
 * my own button functions
 */
$.fn.uoButton = function(icon) {
	$(this).append("<span class='button ui-icon ui-icon-"+icon+"'></span>");
	$(this).addClass("button ui-state-default ui-corner-all");
	$(this).hover(
		function(){ $(this).addClass("ui-state-hover"); }, 
		function(){ $(this).removeClass("ui-state-hover"); }
	);
	$(this).mousedown(function(){ $(this).addClass("ui-state-active"); });
	$(this).mouseup(function(){	$(this).removeClass("ui-state-active"); });
	return $(this);
}
$.fn.uoHoverButton = function(icon) {
	$(this).append("<span class='button ui-icon ui-icon-"+icon+"'></span>");
	$(this).addClass("button ui-corner-all");
	$(this).hover(
		function(){ $(this).addClass("ui-state-hover"); }, 
		function(){ $(this).removeClass("ui-state-hover"); }
	);
	$(this).mousedown(function(){ $(this).addClass("ui-state-active"); });
	$(this).mouseup(function(){	$(this).removeClass("ui-state-active"); });
	return $(this); 
}
$.fn.uoToggleButton = function(iconInactive, iconActive, startActive) {
	$(this).append("<span class='button ui-icon ui-icon-"+iconInactive+"'></span>");
	$(this).addClass("button ui-state-default ui-corner-all")
	$(this).hover(
		function(){ $(this).addClass("ui-state-hover"); }, 
		function(){ $(this).removeClass("ui-state-hover"); }
	);
	
	$(this).click(function() {
		if ($(this).hasClass("ui-state-active")) {
			$(this).removeClass("ui-state-active");
			$("span",this).removeClass("ui-icon-"+iconActive);
			$("span",this).addClass("ui-icon-"+iconInactive);
		} else {
			$(this).addClass("ui-state-active");
			$("span",this).removeClass("ui-icon-"+iconInactive);
			$("span",this).addClass("ui-icon-"+iconActive);
		}
	});
	
	if (startActive)
		$(this).click();
	
	return $(this); 
}

/*
 * Range InputBox
 */
$.fn.uoRangeInput = function(rangeList){
	$(this).each(function() {
		var txt = $(this);
		var btn = $("<div title='add a range value' class='buttonAddRange'></div>");
		btn.uoHoverButton("plus");
		var menuContent = "<ul>"+rangeList+"</ul>";
		btn.menu({ content: menuContent, flyOut: true, onSelect: function(item) {
			if ($(item).attr("title") && $(item).attr("title").length > 0) {
				if ( ! txt.attr("disabled")) {
					if (txt.val().length == 0)
						txt.val($(item).attr("title"));
					else
						txt.val(txt.val() + "\n" + $(item).attr("title"));
					txt.change();
				}
			}
		}});
		txt.after(btn);
	});
}

function reloadRangeMenues() {
	$("#tblStyles .buttonAddRange").remove();
	$("#tblStyles .txtStyleRange").uoRangeInput(rangeMenu.html());
}

/*
 * style table functions
 */
var rowID = 0;
function addStyle(style, ranges) {
	var tblStyles = $("#tblStyles");
	var tblStylesBody = $("#tblStylesBody");
	
	var strTableRow = "<tr>";
	strTableRow += "<td class='dragHandle columnDrag'><span title='drag-and-drop to reorder styles (later ones have higher priority)'></span></td>"; // Order
	strTableRow += "<td class='columnName'><input name='name' class='ui-widget-content ui-corner-all' title='a name for this annotation' /></td>"; // Name
	strTableRow += "<td class='columnShape'>"; // Shape
	strTableRow += "<div id='divShapeWrapper"+rowID+"'>";
		strTableRow += "<input type='radio' class='selShape' value='' name='selShape"+rowID+"' id='selShapeNone"+rowID+"' />";
		strTableRow += "<label for='selShapeNone"+rowID+"' class='lblShapeNone' title='do not change shape'></label>";
		strTableRow += "<input type='radio' class='selShape' value='circ' name='selShape"+rowID+"' id='selShapeCircle"+rowID+"' />"
		strTableRow += "<label for='selShapeCircle"+rowID+"' class='lblShapeCircle' title='use circular shaped residues'></label>";
		strTableRow += "<input type='radio' class='selShape' value='box' name='selShape"+rowID+"' id='selShapeBox"+rowID+"' />";
		strTableRow += "<label for='selShapeBox"+rowID+"' class='lblShapeBox' title='use box shaped residues'></label>";
		strTableRow += "<input type='radio' class='selShape' value='diamond' name='selShape"+rowID+"' id='selShapeDiamond"+rowID+"' />";
		strTableRow += "<label for='selShapeDiamond"+rowID+"' class='lblShapeDiamond' title='use diamond shaped residues'></label>";
		strTableRow += "</div></td>";
	strTableRow += "<td class='columnCC'><input class='txtStyleColorText styleColorPicker' type='text' /></td>"; // Char Color
	strTableRow += "<td class='columnFC'><input class='txtStyleColorFrame styleColorPicker' type='text' /></td>"; // Frame Color
	strTableRow += "<td class='columnBC'><input class='txtStyleColorBackground styleColorPicker' type='text' /></td>"; // Background Color
	strTableRow += "<td class='columnRange'><div class='divStyleRangeWrapper'><textarea class='txtStyleRange ui-widget-content ui-corner-all' cols=15 rows=1 class='ui-widget-content ui-corner-all' title='enter regions to apply this style to using:\n positions (e.g. 93),\n ranges (e.g. 1-12),\n residues (e.g. R),\n sequences (e.g. ELVIS)\n or annotations (click +)'></textarea></div></td>"; // Range
	strTableRow += "<td class='columnActive'><div class='btnActive' title='activate / deactivate this style'></div></td>"; // Active
	strTableRow += "<td class='columnDelete'><div class='btnDelete' title='delete this style'></div></td>"; // Delete
	strTableRow += "</tr>";
	
	var newTableRow = $(strTableRow);
	newTableRow.hover(function() {
		newTableRow.addClass("ui-state-highlight");
		$("span", this.cells[0]).addClass("ui-icon ui-icon-triangle-2-n-s");
	}, function() {
		newTableRow.removeClass("ui-state-highlight");
		$("span", this.cells[0]).removeClass("ui-icon ui-icon-triangle-2-n-s");
    });
	
	$(".btnDelete", newTableRow).uoButton("trash").click(function() {
		defaultStyles=false;
		newTableRow.remove();
		resizeStyleTable();
	});
	$(".txtStyleRange", newTableRow).uoRangeInput(rangeMenu.html());
	
	/*
	 * set the loaded style or default values
	 */
	var styleElements = string2associativeArray(style, "," , ":");
	
	var name = styleElements["n"] ? styleElements["n"] : "";
	$("input[name='name']", newTableRow).val(name);
	
	if (styleElements["inactive"])
		$(".btnActive", newTableRow).uoToggleButton("radio-off","bullet",false);
	else
		$(".btnActive", newTableRow).uoToggleButton("radio-off","bullet",true);
	var shape = styleElements["s"] ? styleElements["s"] : "";
	$("input.selShape[value='"+shape+"']", newTableRow).prop("checked", true);
	
	var charColor = styleElements["cc"] ? styleElements["cc"] : "";
	$("input.txtStyleColorText", newTableRow).val(charColor);
	
	var frameColor = styleElements["fc"] ? styleElements["fc"] : "";
	$("input.txtStyleColorFrame", newTableRow).val(frameColor);
	
	var backgroundColor = styleElements["bc"] ? styleElements["bc"] : "";
	$("input.txtStyleColorBackground", newTableRow).val(backgroundColor);
	
	if (ranges != undefined && typeof(ranges)!="boolean")
		$("textarea.txtStyleRange", newTableRow).val(ranges.replace(/,/g,"\n"));
	
	/*
	 * do final stuff
	 */
	
	$("#divShapeWrapper"+rowID, newTableRow).buttonset();
	$(".lblShapeNone", newTableRow).addClass("ui-corner-all");
	$(".lblShapeCircle", newTableRow).addClass("ui-corner-left");
	$(".lblShapeNone span", newTableRow).addClass("ui-icon ui-icon-cancel");
	
	$(".styleColorPicker", newTableRow).colorPicker(true); // true...allowNoChange option
	$("div.color_picker", newTableRow).hover(
		function(){ $(this).addClass("ui-state-hover"); }, 
		function(){ $(this).removeClass("ui-state-hover"); }
	);
	
	$("input, textarea", newTableRow).change( function() {
		defaultStyles=false;
		refresh();
	});
	$(".btnActive, .btnDelete", newTableRow).click( function() {
		defaultStyles=false;
		refresh();
	});

	tblStylesBody.append(newTableRow);
	rowID++;
	tblStylesBody.tableDnDUpdate();
	
	resizeStyleTable();
	//resizeProteinTable();
}
function resizeStyleTable(){
	var wrapperHeight = $("#divStyleTableWrapper").height();
	var headerHeight = $("#tblStylesHead").height();
	var footerHeight = $("#tblStylesFoot").height();
	var tbodyHeight = wrapperHeight - headerHeight - footerHeight - 4;
	$("#tblStylesBody").css("height", tbodyHeight);
	//var trHeight = $("#tblStylesBody tr").height();
	//$("#tblStyles textarea").css("height", (tbodyHeight / $("#tblStylesBody tr").length) - 20);
}

function clearStyles() {
	var tblStylesBody = $("#tblStylesBody");
	tblStylesBody.empty();
	tblStylesBody.tableDnDUpdate();
	resizeStyleTable();
}

function getStyles() {
	var availableStyles = new Array();
	$("#tblStylesBody tr").each(function() {
		var styleElements = new Array();
		var active = $("div.btnActive", this).hasClass("ui-state-active");
		if (active) {
			var styleElements = new Array();
			var name = $("input[name='name']", this).val();
			if (name.length>0)
				styleElements.push("n:"+name);
			var shape = $("input.selShape:checked", this).val();
			if (shape.length>0)
				styleElements.push("s:"+shape);
			var charColor = $("input.txtStyleColorText", this).val();
			if (charColor.length>0)
				styleElements.push("cc:"+charColor);
			var frameColor = $("input.txtStyleColorFrame", this).val();
			if (frameColor.length>0)
				styleElements.push("fc:"+frameColor);
			var backgroundColor = $("input.txtStyleColorBackground", this).val();
			if (backgroundColor.length>0)
				styleElements.push("bc:"+backgroundColor);
			if (styleElements.length > 0) {
				var key = styleElements.join(",");
				availableStyles[key] = name + " (" + key + ")";
			}
		}
	});
	return availableStyles;
}

/*
 * peptideCutter
 */
function loadProteases() {
	//$.ajax({
	//	dataType: "json",
	//	url: "proteases.json",
	//	async: false,
	//	error: function() { alert("could not load proteases"); },
	//	success: function(data) {
			$.each(proteases, function(key, val) {
				$("#lstProtease").append("<option value='" + key + "'>" + val + "</option>");
			});
	//	}
	//});
}

/*
 * zooming functions
 */
function zoomIn() {
	var factor = 1+zoom/100;
	svgDOM.setAttribute("height", svgDOM.getAttribute("height") * factor);
	svgDOM.setAttribute("width", svgDOM.getAttribute("width") * factor);
}
function zoomOut() {
	if (svgDOM.getAttribute("height") <= svgContainer.height() && svgDOM.getAttribute("width") <= svgContainer.width())
		return;
	var factor = 1-zoom/100;
	svgDOM.setAttribute("height", svgDOM.getAttribute("height") * factor);
	svgDOM.setAttribute("width", svgDOM.getAttribute("width") * factor);
}

/*
 * svg functions
 */
function loadSVG() {
	svg.clear(true);
	if (queryString.length>0) {
		svgContainer.addClass("loading");
		svg.load(createPath+"?"+queryString, {onLoad: svgLoaded}); // initialize the container div as svg container
	}
}
function svgLoaded(svg, error) { // Callback after loading external svg	
	if(error) {
		$.get(createPath+"?"+queryString).error(function(e) {
			if (e.responseText.indexOf("Error: java.lang.Exception: Found multiple UniProt entries")==0) {
				var searched = e.responseText.substring(e.responseText.indexOf("'")+1, e.responseText.indexOf("'", e.responseText.indexOf("'")+1));
				$('#lblSearched').text("'"+searched+"'");
				$('#lstResults').children().remove();
				var arrLines = e.responseText.split("\n");
				arrLines.shift();
				arrLines.map( function(item) {
					var arrItems = item.split(" ");
					$('#lstResults').append('<option value="' + arrItems[0] + '">' + item + '</option>');
				});
				$('#divUniprotDialog').dialog('open');
			} else {
				alert(e.responseText);
			}
			svgContainer.removeClass("loading");
			//$("#divContainer").tabs("option", "active", 0);
			//$("#divContainer").tabs("option", "disabled", [1,2,3]);
		});
	} else {
		svgContainer.removeClass("loading");
	}
	
	svgDOM = svg.root();
	
	svgFitToScreen();
	
	// read metadata from svg...
	query.up = $("svg #uniprotID").text();
	query.seq = $("svg #sequence").text();
	
	svgContainer.mouseover(function() {
  		lblStatus.text("");
		return false; // prevent bubbling
	});
	svgContainer.click(function() {
  		svgAAunselect();
		//return false; // prevent bubbling
	});
	
	// add the selection filter
	var filterSelected = svg.filter($("defs"), 'selected', -5, -5, 10, 10, {filterUnits: 'objectBoundingBox'});
	svg.filters.morphology(filterSelected, 'dilated', 'SourceGraphic', 'dilate', 0.3);
	svg.filters.colorMatrix(filterSelected, 'colored', 'dilated', 'matrix', [
										   // input: r  g  b  a      output:  
													[0, 0, 0, 1, 0], // red
													[0, 0, 0, 0, 0], // green
													[0, 0, 0, 0, 0], // blue
													[0, 0, 0, 1, 0]]); // alpha
	svg.filters.gaussianBlur(filterSelected, 'blured', 'colored', 0.4);
	svg.filters.merge(filterSelected, '', ['blured', 'SourceGraphic']);
	
	// add the highlight filter
	var filterHighlighted = svg.filter($("defs"), 'highlighted', -5, -5, 10, 10, {filterUnits: 'objectBoundingBox'});
	svg.filters.morphology(filterHighlighted, 'dilated', 'SourceGraphic', 'dilate', 0.3);
	svg.filters.colorMatrix(filterHighlighted, 'colored', 'dilated', 'matrix', [
										   // input: r  g  b  a      output:  
													[0, 0, 0, 1, 0], // red
													[0, 0, 0, 0.5, 0], // green
													[0, 0, 0, 0.5, 0], // blue
													[0, 0, 0, 1, 0]]); // alpha
	svg.filters.gaussianBlur(filterHighlighted, 'blured', 'colored', 0.4);
	svg.filters.merge(filterHighlighted, '', ['blured', 'SourceGraphic']);
	
	// add interactivity to aminoacids
	var aaCount = 0;
	$("#page1 > circle, #page1 > rect").each( function() {
		if ($(this).attr("id").substring(0, 2) == "aa")
			aaCount++;
	});
	console.log("found "+aaCount+" residues")
	$("#page1 > circle, #page1 > rect").each( function() {
		var id = $(this).attr("id");
		if (id.substring(0, 2) == "aa") {
			var symbol = $(this);
			var letter = symbol.next();
			var aaIndex = parseInt(id.substring(2, id.indexOf("_")));
			var title = symbol.children().text();
			
			highlightAA = function(){
				if (!symbol.attr("filter") || symbol.attr("filter") == "") 
					symbol.attr("filter", "url(#highlighted)");
				
				if (aaSelectionStart > -1 && aaSelectionEnd==-1) {
					var from = (aaSelectionStart < aaIndex) ? aaSelectionStart : aaIndex;
					var to = (aaSelectionStart < aaIndex) ? aaIndex : aaSelectionStart;
					lblStatus.text("click to select from " + (from+1) + " to " + (to+1));
					for (var i=0; i<aaCount; i++) {
						if (i==aaSelectionStart)
							continue; // is already selected
						else if (i>=from && i<=to)
							$("#page1 > #aa"+i+"_symbol").attr("filter", "url(#highlighted)");
						else
							$("#page1 > #aa"+i+"_symbol").attr("filter", "");
					}
				} else {
					lblStatus.text(title);
				}
				return false; // prevent bubbling
			}
			unhighlightAA = function(){
				if (symbol.attr("filter") == "url(#highlighted)") 
					symbol.attr("filter", "");
					
				//status.text("leave");
				for (var i=0; i<aaCount; i++) {
					if ($("#page1 > #aa"+i+"_symbol").attr("filter") == "url(#highlighted)")
						$("#page1 > #aa"+i+"_symbol").attr("filter", "");
				}
				
				return false; // prevent bubbling
			}
			
			
			symbol.hover(highlightAA, unhighlightAA);
			letter.hover(highlightAA, unhighlightAA);
			
			symbol.click(function(){return svgAAselect(aaIndex);});
			letter.click(function(){return svgAAselect(aaIndex);});
		}
	});
	// when click - set a variable for beginSelectionAA
	// when move check for a begin, if present highlight all to begin, else only current
	// when click again on another AA, select all in between
	//					on same AA, unselect
	//					on anywhere, reset beginSelectionAA
	// remember to reset beginSelectionAA on reload of svg
	// 
	// synchronize with selection in sequence textbox!
	
	$(document).trigger('svgLoaded');
}

function svgAAselect(aaIndex) {
	if (aaSelectionStart > -1 && aaSelectionEnd == -1) {
		var from = (aaSelectionStart < aaIndex) ? aaSelectionStart : aaIndex;
		var to = (aaSelectionStart < aaIndex) ? aaIndex : aaSelectionStart;
		aaSelectionStart = from;
		aaSelectionEnd = to;
		for (var i=aaSelectionStart; i<=aaSelectionEnd; i++) // select all
			$("#page1 > #aa"+i+"_symbol").attr("filter", "url(#selected)");
		$(".rangeMenuItemSelectedRange").attr("title",(from+1)+"-"+(to+1));
		$(".rangeMenuItemSelectedRange", rangeMenu).attr("title",(from+1)+"-"+(to+1));
		$(".rangeMenuItemSelectedRange").show();
		$(".rangeMenuItemSelectedRange", rangeMenu).show();
	} else {
		svgAAunselect();
		
		//symbol.attr("filter", "url(#selected)");
		$("#page1 > #aa"+aaIndex+"_symbol").attr("filter", "url(#selected)");
		aaSelectionStart = aaIndex;
		aaSelectionEnd = -1;
	}
	return false; // prevent bubbling
}
function svgAAunselect(){
	$("#page1 > circle, #page1 > rect").each(function() {
		if ($(this).attr("id").substring(0, 2) == "aa")
			$(this).attr("filter", "");
	});
	aaSelectionStart = -1;
	aaSelectionEnd = -1;
	$(".rangeMenuItemSelectedRange").hide();
	$(".rangeMenuItemSelectedRange", rangeMenu).hide();
	
	$("#tblProteins tr.peptideRow").removeClass("selected");
}

function svgFitToScreen() {
	// calculate & set correct svg width & height
	if (svgDOM.getAttribute("viewBox") != null) { 
		var viewBox = svgDOM.getAttribute("viewBox").split(" ");
		var vAspectRatio = viewBox[2]/viewBox[3]; // viewbox's width/height
		var containerWidth = svgContainer.width();
		var containerHeight = svgContainer.height();
		var containerAspectRatio = containerWidth/containerHeight;	
		
		var svgWidth;
		var svgHeight;
		if (vAspectRatio > containerAspectRatio) { // svg breiter als container
			svgWidth = containerWidth;
			svgHeight = svgWidth / vAspectRatio;
		} else { // svg hoeher als container
			svgHeight = containerHeight;
			svgWidth = svgHeight * vAspectRatio;
		}
		svgDOM.setAttribute("height", svgHeight);
		svgDOM.setAttribute("width", svgWidth);
	}
}

// shows a demo animation
function loadDemo() {
	demoUrl = (useSvgAnimation ? "demo.svg" : "demoLow.svg");
	svg.load(demoUrl, {onLoad: function(svg, error) {
		if (!error) {
			svgDOM = svg.root();
			svgFitToScreen();
		}
	}});
}


/*
 * Read a page's GET URL variables and return them as an associative array.
 *   see http://jquery-howto.blogspot.com/2009/09/get-url-parameters-values-with-jquery.html
 */
$.extend({
	getUrlVars: function(){
		var vars = [], hash;
		if (window.location.href.indexOf('?') != -1) {
			var hashes = window.location.href.slice(window.location.href.indexOf('?') + 1).split('&');
			for (var i = 0; i < hashes.length; i++) {
				hash = hashes[i].split('=');
				//vars.push(hash[0]);
				vars[hash[0]] = hash[1];
			}
		}
		return vars;
	},
	getUrlVar: function(name){
		return $.getUrlVars()[name];
	}
});

function string2associativeArray(str, sepArr, sepKeyVal) {
	var vars = [];
	var arr = str.split(sepArr);
	for (var i=0; i<arr.length; i++) {
		if (arr[i].length>0) {
			var key = arr[i].split(sepKeyVal)[0];
			var val = arr[i].split(sepKeyVal)[1];
			if (val)
				vars[key] = val;
			else
				vars[key] = true;
		} 
	}
	return vars;
}

function showMessageInfo(title, message, time) {
	$("#messageTitle").html(title);
	$("#messageBody").html(message);
	$("#message > div").removeClass("ui-state-error").addClass("ui-state-highlight");
	$("#messageIcon").removeClass("ui-icon-alert").addClass("ui-icon-info");
	if (time>0)
		$("#message").show("drop",{direction:"up"}).delay(time*1000).hide("drop",{direction:"up"});
	else
		$("#message").show("drop",{direction:"up"});
}
function showMessageAlert(title, message, time) {
	$("#messageTitle").html(title);
	$("#messageBody").html(message);
	$("#message > div").removeClass("ui-state-highlight").addClass("ui-state-error");
	$("#messageIcon").removeClass("ui-icon-info").addClass("ui-icon-alert");
	if (time>0)
		$("#message").show("drop",{direction:"up"}).delay(time*1000).hide("drop",{direction:"up"});
	else
		$("#message").show("drop",{direction:"up"});
}

function clearProteins() {
	tblProteins.empty();
	tblProteins.data({batch: "", count: 0});
}

function unselectProteins() {
	$("#tblProteins tr.selected").removeClass("selected");
}

function showProteins() {
	isMulti = true;
	proteinFilter="";
	$("#proteinFilter").val("");
	tblProteins.treetable({ expandable: true, clickableNodeNames: true }, true);
	if ($("#divSequenceSelection").is(':hidden')) {
		$("#divSequenceSelection").show();
		$("div.vsplitbar").show();
		$("#divContent").css("left",$("#divContent").data("previousLeft"));
		$("#divContent").resize();
	}
	$("#tblProteins tr").first().click();
}

function addProtein(name, seq, peps, mods, displayPeptideCount) {
	var index = tblProteins.data("count");
	
	var proteinIsUniprot = (seq===null);
	
	var proteinRow = $("<tr data-tt-id='"+index+"'></tr>");
	proteinRow.append($("<td>"+name+"</td>"));
	
	// store all info for batch visualization purposes
	var batchString;
	
	if (proteinIsUniprot) {
		if (displayPeptideCount)
			proteinRow.append($("<td>"+Object.keys(peps).length+(Object.keys(peps).length==1 ? " peptide" : " peptides")+"</td>"));
		else 
			proteinRow.append($("<td></td>"))
		batchString = "title="+name+"&up="+name;
	} else {
		proteinRow.append($("<td>"+seq.length+"aa</td>"));
		batchString = "title="+name+"&seq="+seq;
	}
	
	tblProteins.append(proteinRow);
	tblProteins.data("count", index+1);
	$("#lblCountProteins").text(index+1);
	
	var pepSequences = {};
	
	if (proteinIsUniprot) {
		for(var i = 0; i < Object.keys(peps).length; i++){
			var pepDisplay;
			var pepSequence;
			if (Array.isArray(peps)) { // array of peptides (manual input)
			 	pepSequence = peps[i];
			 	pepDisplay = pepSequence;
			} else { // parsed proteomics file object
			 	pepDisplay = Object.keys(peps)[i];
				pepSequence = peps[pepDisplay];
			}
			pepSequences[pepSequence] = true;
			
			var peptideRow = $("<tr data-tt-id='"+index+"."+i+"' data-tt-parent-id='"+index+"' class='peptideRow'></tr>");
			peptideRow.append($("<td>"+pepDisplay+"</td><td></td>"));
			peptideRow.data("seq",pepSequence);
			// highlight selected row
			peptideRow.click(function() {
				$("#tblProteins tr.selected").not(this).not(proteinRow).removeClass("selected");
				var pepSeq = $(this).data("seq");
				var peptideRow = $(this);
				// check whether protein is already selected
				var highlightPeptide = function() {
					// highlight peptide
					svgAAunselect();
					var j = query.seq.toLowerCase().replace(/i/g,"l").indexOf( pepSeq.toLowerCase().replace(/i/g,"l") );
					if (j>=0) {
						svgAAselect(j);
						svgAAselect(j+pepSeq.length-1);
					}
					peptideRow.addClass("selected");
				}
				if (proteinRow.hasClass("selected")) {
					highlightPeptide();
				} else {
					// select and load protein first
					proteinRow.click();
					$(document).one("svgLoaded", highlightPeptide);
				}
			});
			tblProteins.append(peptideRow);
		}
		
		// save batch string for batch visualization
		if (Object.keys(pepSequences).length > 0) {
			batchString += "&peptides=" + Object.keys(pepSequences).join(",");
		}
		if (Object.keys(mods).length > 0) {
			for(var i=0; i<Object.keys(mods).length; i++) {
				var strMod = Object.keys(mods)[i];
				var strPeps = Object.keys(mods[strMod]);
				strMod = generateModString(strMod);
				batchString += "&" + strMod + "=" + strPeps.join(",");
			}
		}
	}
	
	tblProteins.data("batch", tblProteins.data("batch") + batchString + "\n");
	
	// highlight selected row
	proteinRow.click( function() {
		$("#tblProteins tr.selected").not(this).removeClass("selected");
		if (! $(this).hasClass("selected")) {
			$(this).addClass("selected");
			
			if (proteinIsUniprot) {
				$("#txtID").val(name);
				$("#frmID").trigger("submit", {peps:Object.keys(pepSequences), mods:mods});
			} else {
				$("#txtSequence").val(seq);
				$("#frmSequence").submit();
			}
			$(this).addClass("selected"); // submit will unselect all -> re-select protein
		}
	});
}

function loadProteinsFromSequences(e) {
	e.preventDefault();
	isUniprot = false;
	if (isMulti)
		unselectProteins();
	$(".rangeMenuItemProteomics").hide();
	$(".rangeMenuItemProteomics", rangeMenu).hide();
	
	var arrEntries = $("#txtSequence").val().split(">");
	if (arrEntries.length > 2) {
		clearProteins();
		var name;
		var entry;
		for (var i=1; i<arrEntries.length; i++) { // first will always be empty
			var entry = ">" + arrEntries[i].trim();
			var name = arrEntries[i].split(/[\r\n]+/)[0].trim();
			var seq = entry.trim().replace(/^>.*/g,"").replace(/\s/g,"");
			addProtein(name, seq, null, null, false);
		}
		showProteins();		
	} else { // only one sequence given -> display
		refresh();
	}
	return false;
}

function loadProteinsFromIdList(e) {
	if (/^\s*$/.test($("#txtProteinIDs").val())) {
		e.preventDefault();
		alert("Please enter at least one protein identifier!");
		return false;
	}
	
	e.preventDefault();
	$("#dlgProteinIDs").dialog('close');
	
	loadProteinsFromProteomics($("#txtProteinIDs").val());
	
	return false;
}


function loadProteinsFromProteomics(e) {
	var files;
	if (Array.isArray(e)) {
		files = e; // file(s) from server
	} else if (typeof e == "object") {
		e.preventDefault(); // local files
		files = $("#inpProteomics")[0].files;
		if (files.length == 0) {
			alert("Please select a file first!");
			return false;
		}
		$("#dlgProteomics").dialog('close');
	} else if (typeof e === "string") { // local id-list (with optional peptides)
		files = e;
	}
	isUniprot = true;
	clearProteins();
	
	var parser = new Parser(files, function() {
		for(var i = 0, protein; protein = Object.keys(this.proteins)[i]; i++) {
			addProtein(protein, null, this.proteins[protein].modPeptides, this.proteins[protein].mods, true);
		}
		
		$(".rangeMenuItemModifications", rangeMenu).show();
		$(".rangeMenuItemModifications + ul", rangeMenu).empty();
		$(".rangeMenuItemModifications + ul", rangeMenu).append("<li><a href='#' title='EX.MODALL'>all modifications</a></li>");
		for(var i = 0, mod; mod = Object.keys(this.modsGlobal)[i]; i++) {
			key = "EX." + generateModString(mod);
			$(".rangeMenuItemModifications + ul", rangeMenu).append("<li><a href='#' title='"+key+"'>"+mod+"</a></li>");
		}
		
		svgContainer.removeClass("loading");
		showProteins();
	});
	
	svg.clear(true);
	svgContainer.addClass("loading");
	
	return false;
}

function generateModString(mod) {
	return "mod" + mod.replace(/\W/g, "_");
}

function reset() {
	if (confirm('You will loose all the settings made so far. Do you really want to reset?')) {
		defaultStyles = true;
		queryString = "";
		clearProteins();
		isMulti = false;
		isUniprot = true;
		$("a[href='#tabSequence']").click();
		//$("#divSequenceInput").tabs("option", "active", 0);
		$("#divContainer").tabs("option", "disabled", [1,2,3]);
		location.hash = queryString;
		loadFromQueryString();
		loadSVG();
	}
	return false;
}

/* Modernizr 2.6.2 (Custom Build) | MIT & BSD
 * Build: http://modernizr.com/download/#-inlinesvg-smil-svg-file_api-forms_formattribute-svg_filters
 */
;window.Modernizr=function(a,b,c){function u(a){i.cssText=a}function v(a,b){return u(prefixes.join(a+";")+(b||""))}function w(a,b){return typeof a===b}function x(a,b){return!!~(""+a).indexOf(b)}function y(a,b,d){for(var e in a){var f=b[a[e]];if(f!==c)return d===!1?a[e]:w(f,"function")?f.bind(d||b):f}return!1}var d="2.6.2",e={},f=b.documentElement,g="modernizr",h=b.createElement(g),i=h.style,j,k={}.toString,l={svg:"http://www.w3.org/2000/svg"},m={},n={},o={},p=[],q=p.slice,r,s={}.hasOwnProperty,t;!w(s,"undefined")&&!w(s.call,"undefined")?t=function(a,b){return s.call(a,b)}:t=function(a,b){return b in a&&w(a.constructor.prototype[b],"undefined")},Function.prototype.bind||(Function.prototype.bind=function(b){var c=this;if(typeof c!="function")throw new TypeError;var d=q.call(arguments,1),e=function(){if(this instanceof e){var a=function(){};a.prototype=c.prototype;var f=new a,g=c.apply(f,d.concat(q.call(arguments)));return Object(g)===g?g:f}return c.apply(b,d.concat(q.call(arguments)))};return e}),m.svg=function(){return!!b.createElementNS&&!!b.createElementNS(l.svg,"svg").createSVGRect},m.inlinesvg=function(){var a=b.createElement("div");return a.innerHTML="<svg/>",(a.firstChild&&a.firstChild.namespaceURI)==l.svg},m.smil=function(){return!!b.createElementNS&&/SVGAnimate/.test(k.call(b.createElementNS(l.svg,"animate")))};for(var z in m)t(m,z)&&(r=z.toLowerCase(),e[r]=m[z](),p.push((e[r]?"":"no-")+r));return e.addTest=function(a,b){if(typeof a=="object")for(var d in a)t(a,d)&&e.addTest(d,a[d]);else{a=a.toLowerCase();if(e[a]!==c)return e;b=typeof b=="function"?b():b,typeof enableClasses!="undefined"&&enableClasses&&(f.className+=" "+(b?"":"no-")+a),e[a]=b}return e},u(""),h=j=null,e._version=d,e}(this,this.document),Modernizr.addTest("filereader",function(){return!!(window.File&&window.FileList&&window.FileReader)}),Modernizr.addTest("formattribute",function(){var a=document.createElement("form"),b=document.createElement("input"),c=document.createElement("div"),d="formtest"+(new Date).getTime(),e,f=!1;return a.id=d,document.createAttribute&&(e=document.createAttribute("form"),e.nodeValue=d,b.setAttributeNode(e),c.appendChild(a),c.appendChild(b),document.documentElement.appendChild(c),f=a.elements.length===1&&b.form==a,c.parentNode.removeChild(c)),f}),Modernizr.addTest("svgfilters",function(){var a=!1;try{a=typeof SVGFEColorMatrixElement!==undefined&&SVGFEColorMatrixElement.SVG_FECOLORMATRIX_TYPE_SATURATE==2}catch(b){}return a});

