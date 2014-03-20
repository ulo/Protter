function Parser (files, onDone) {
	var parser = this;
	
	this.files = files;
	this.onDone = onDone;
	this.proteins = {};  // protein "object"
	this.modsGlobal = {}; // global collection of occuring mods
	
	this.ensureProt = function (strProtein) {
		if (parser.proteins[strProtein] == undefined) {
			parser.proteins[strProtein] = {};
			parser.proteins[strProtein].modPeptides = {}; // mod-peptide object
			parser.proteins[strProtein].peptides = {}; // peptide object
			parser.proteins[strProtein].mods = {}; // mod object
		}
	}
	
	this.addPeptide = function (strProtein, strPeptide, strPeptideMod) {
		parser.ensureProt(strProtein);
		parser.proteins[strProtein].peptides[strPeptide] = true;
		parser.proteins[strProtein].modPeptides[strPeptideMod] = strPeptide;
	}
	
	this.addMod = function (strProtein, mod, modPep) {
		parser.ensureProt(strProtein);
		if (parser.proteins[strProtein].mods[mod] == undefined)
			parser.proteins[strProtein].mods[mod] = {}; // mod object
		parser.proteins[strProtein].mods[mod][modPep] = true;
		parser.modsGlobal[mod] = true;
	}
	
	this.readFile = function (fileIndex) {
		if (typeof this.files == "string") {
			console.log("processing input...");
			parser.parse("idlist.tsv", this.files);
			console.log("done!");
			this.onDone();
			return;
		} else {
			if (fileIndex >= this.files.length) {
				this.onDone();
				return;
			}
			var f = this.files[fileIndex];
			if (typeof f === "string") {
				console.log("reading remote file '"+f+"'...");
				$.ajax({url: 'upload/'+f, async: false})
					.done(function(data, textStatus, jqXHR) {
						parser.parse(f, data);
						parser.readFile(fileIndex+1); // read next...
						console.log("done!");
					})
					.fail(function(jqXHR, textStatus, errorThrown) {
						console.log("error retrieving file '"+f+"': "+textStatus);
					});
			} else {
				console.log("reading local file '"+f.name+"'...");
				var reader = new FileReader();
				reader.onloadend = function(e) {
					if (e.target.readyState == FileReader.DONE) { // DONE == 2
						parser.parse(f.name, e.target.result);
					}
					console.log("done!");
					parser.readFile(fileIndex+1); // read next...
				};
				reader.readAsText(f);
			}
		}
	};
	
	/*
	 * generic parse function	
	 */
	
	this.parse = function (filename, content) {
		var lines = content.split(/[\r\n]+/g); // tolerate both Windows and Unix linebreaks
		if (lines[0].length==0)
			lines.shift(); // remove empty first line
		try {
			if (lines.length==0)
				throw "file is empty";
			if (filename.match(/prot\.xls$/i))
				parser.parseTPPprot(lines);
			else if (filename.match(/pep\.xls$/i))
				parser.parseTPPpep(lines);
			else if (filename.match(/\.txt$/i))
				parser.parseMaxQuant(lines);
			else if (filename.match(/\.tsv$/i))
				parser.parseIdList(lines);
			else if (filename.match(/\.csv$/i)) {
				if (lines[0].indexOf("\"--------------------------------------------------------\"") > 0)
					parser.parseMascot(lines);
				else
					parser.parseSkyline(lines);
			} else
				throw "unknown file type";
		} catch(err) {
			alert("Cannot parse file '"+filename+"': "+err);
		}
		lines=null;	
	}
	
	/*
	 * specific parse functions	
	 */
	
	this.parseTPPpep = function (lines) {
		var header = lines[0].split(/\t/g);
		var i_peptide = header.indexOf("peptide");
		var i_protein = header.indexOf("protein");
		if (i_peptide<0 || i_protein<0) {
			throw "File does not seem to be a proper TPP PeptideProphet .pep.xls file.";
		}
		for(var j = 1, line; line = lines[j]; j++) {
			var elems = line.split(/\t/g);
			var strPeptideMod = elems[i_peptide]; // K.LLDEN[115.03]M[147.04]AR.I | K.n[58.03]N[115.03]GVN[115.03]GTGEN[115.03]GR.K 
			strPeptideMod = strPeptideMod.substring(2, strPeptideMod.length-2); // LLDEN[115.03]M[147.04]AR
			var strProtein = elems[i_protein].split(',')[0]; // first of protein groups
			var strPeptide = strPeptideMod.replace(/\[.+?\]/g,""); // remove all mods
			if (strPeptideMod.indexOf("n[")==0)
				strPeptide = strPeptide.substring(1);
			parser.addPeptide(strProtein, strPeptide, strPeptideMod.replace(/\[/g, "<span class='mod'>").replace(/\]/g, "</span>"));
			
			var k = 0;
			if (strPeptideMod.indexOf("n[")==0) 
				strPeptideMod = strPeptideMod.substring(1);
			// store all modified peptide sequences, per modification [cannot get the positions]
			while((k = strPeptideMod.indexOf('[', k)) >= 0) {
				var l = strPeptideMod.indexOf(']', k);
				var mod = (k==0 ? "n":"") + strPeptideMod.substring(k-1, l+1);
				var modPep = strPeptideMod.substring(0, k-1) + "(" + strPeptideMod.substring(k-1, k) + ")" + strPeptideMod.substring(l+1);
				if (k==0)
					modPep = "(" + strPeptideMod.substring(l+1, l+2) + ")" + strPeptideMod.substring(l+2); // n-terminal mods will be attributed to first AA
				//TODO: support cterminal mods
				modPep = modPep.replace(/\[.+?\]/g, ""); // remove all other mods
				parser.addMod(strProtein, mod, modPep);
				k++;
			}
		}
	};
	
	this.parseTPPprot = function (lines) {
		var header = lines[0].split(/\t/g);
		var i_peptide = header.indexOf("peptide sequence");
		var i_protein = header.indexOf("protein");
		if (i_peptide<0 || i_protein<0) {
			throw "File does not seem to be a proper TPP ProteinProphet .prot.xls file.";
		}
		for(var j = 1, line; line = lines[j]; j++) {
			var elems = line.split(/\t/g);
			var strPeptideMod = elems[i_peptide]; // e.g. IVES[167]AST[181]HIEDAHSNLK
			var strProtein = elems[i_protein].split(',')[0]; // first of protein groups
			var strPeptide = strPeptideMod.replace(/\[.+?\]/g,""); // remove all mods
			if (strPeptideMod.indexOf("n[")==0)
				strPeptide = strPeptide.substring(1);
			parser.addPeptide(strProtein, strPeptide, strPeptideMod.replace(/\[/g, "<span class='mod'>").replace(/\]/g, "</span>"));
			
			var k = 0;
			if (strPeptideMod.indexOf("n[")==0) 
				strPeptideMod = strPeptideMod.substring(1);
			// store all modified peptide sequences, per modification [cannot get the positions]
			while((k = strPeptideMod.indexOf('[', k)) >= 0) {
				var l = strPeptideMod.indexOf(']', k);
				var mod = (k==0 ? "n":"") + strPeptideMod.substring(k-1, l+1);
				var modPep = strPeptideMod.substring(0, k-1) + "(" + strPeptideMod.substring(k-1, k) + ")" + strPeptideMod.substring(l+1);
				if (k==0)
					modPep = "(" + strPeptideMod.substring(l+1, l+2) + ")" + strPeptideMod.substring(l+2); // n-terminal mods will be attributed to first AA
				//TODO: support cterminal mods
				modPep = modPep.replace(/\[.+?\]/g, ""); // remove all other mods
				parser.addMod(strProtein, mod, modPep);
				k++;
			}
		}
	};
	
	this.parseMascot = function (lines) {
		var section = "";
		var modsByID = {};
		var i_protFamilyMember;
		var i_protein;
		var i_peptide;
		var i_modString;
		for(var j = 1, line; line = lines[j]; j++) {
			if (line.indexOf("\"Variable modifications\"")==0) {
				section = "varmods";
				j++; // skip header
			} else if (line.indexOf("\"Protein hits\"")==0) {
				section = "proteins";
				var header = lines[++j].split(/,/g); // header
				i_protFamilyMember = header.indexOf("prot_family_member");
				i_protein = header.indexOf("prot_acc");
				i_peptide = header.indexOf("pep_seq");
				i_modString = header.indexOf("pep_var_mod_pos");
			} else if (line.indexOf("\"--------------------------------------------------------\"")>0) {
				section = "";
			} else if (section == "varmods") {
				var elems = line.split(/,/g);
				var modID = elems[0];
				var modName = elems[1].replace(/\"/g, "");
				modsByID[modID] = modName;
			} else if (section == "proteins") {
				// parse csv encoded line
				var elems = [];
				for (var startPos=0, endPos=0; endPos<line.length; startPos=endPos+1) {
					if (line.charAt(startPos)=='"') { 
						endPos = line.indexOf(',', line.indexOf('"', startPos+1)); 
					} else {
						endPos = line.indexOf(',', startPos);
					}
					if (endPos==-1) { endPos = line.length; }
					elems.push(line.substring(startPos, endPos));
				}
				
				if (elems[i_protFamilyMember].length==0)
					continue; // subsumable protein entry; otherwise 1,2,3...
				var strProtein = elems[i_protein].replace(/\"/g, "");
				var strPeptide = elems[i_peptide];
				var strMod = elems[i_modString]; // 0.000002010000.0
				
				// store all modified peptide sequences per modification
				if (strMod.length>0) {
					var strPeptideMod = "";
					//TODO: support terminal modifications
					strMod = strMod.substring(2, strMod.length-2);
					for (var i = 0; i < strMod.length; i++) {
						strPeptideMod += strPeptide.charAt(i);
						var mod = modsByID[strMod.charAt(i)];
						if (mod != undefined) {
							strPeptideMod += "<span class='mod'>" + mod + "</span>";
							var modPep = strPeptide.substring(0, i) + "(" + strPeptide.substring(i, i+1) + ")" + strPeptide.substring(i+1);
							parser.addMod(strProtein, mod, modPep);
						}
					}
					parser.addPeptide(strProtein, strPeptide, strPeptideMod);
				} else { 
					parser.addPeptide(strProtein, strPeptide, strPeptide);
				}
			}
		}
	};
	
	this.parseMaxQuant = function (lines) {
		var header = lines[0].toLowerCase().split(/\t/g);
		var i_peptide = header.indexOf("sequence");
		var i_peptideMod = header.indexOf("modified sequence");
		var i_protein = header.indexOf("leading razor protein");
		if (i_peptide<0 || i_protein<0 || i_peptideMod<0) {
			throw "File does not seem to be a proper MaxQuant evidence.txt file.";
		}
		for(var j = 1, line; line = lines[j]; j++) {
			var elems = line.split(/\t/g);
			var strPeptide = elems[i_peptide]; // AHVIIGNISENMTIYGFDK
			var strPeptideMod = elems[i_peptideMod]; // e.g. _AHVIIGNISEN(de)M(ox)TIYGFDK_
			var strProtein = elems[i_protein].split(';')[0]; // first of protein groups
			parser.addPeptide(strProtein, strPeptide, strPeptideMod.substring(1, strPeptideMod.length-1).replace(/\(/g, "<span class='mod'>").replace(/\)/g, "</span>"));
			
			var k = 0;			
			// store all modified peptide sequences, per modification
			while((k = strPeptideMod.indexOf('(', k)) >= 0) {
				var l = strPeptideMod.indexOf(')', k);
				var mod = strPeptideMod.substring(k-1, l+1);
				var modPep = strPeptideMod.substring(0, k-1) + "(" + strPeptideMod.substring(k-1, k) + ")" + strPeptideMod.substring(l+1);
				//TODO: support terminal mods
				modPep = modPep.replace(/\(.+?\)/g, ""); // remove all other mods
				parser.addMod(strProtein, mod, modPep);
				k++;
			}
		}
	}
	
	this.parseSkyline = function (lines) {
		var header = lines[0].split(/,/g);
		var i_peptide = header.indexOf("PeptideSequence");
		var i_peptideMod = header.indexOf("PeptideModifiedSequence");
		var i_protein = header.indexOf("ProteinName");
		var i_proteinSeq = header.indexOf("ProteinSequence");
		if (i_peptide<0 || i_peptideMod<0 || i_protein<0) {
			throw "File does not seem to be a proper Skyline report.";
		}
		for(var j = 1, line; line = lines[j]; j++) {
			var elems = line.split(/,/g);
			var strPeptide = elems[i_peptide];
			var strPeptideMod = elems[i_peptideMod]; // e.g. AGLC[+57]QTFVYGGC[+57]R
			var strProtein = elems[i_protein]; // protein groups??
			var strProteinSeq = elems[i_proteinSeq];
			parser.addPeptide(strProtein, strPeptide, strPeptideMod.replace(/\[/g, "<span class='mod'>").replace(/\]/g, "</span>"));
			// protein sequence ??
			
			var k = 0;			
			// store all modified peptide sequences, per modification
			while((k = strPeptideMod.indexOf('[', k)) >= 0) {
				var l = strPeptideMod.indexOf(']', k);
				var mod = strPeptideMod.substring(k-1, l+1);
				var modPep = strPeptideMod.substring(0, k-1) + "(" + strPeptideMod.substring(k-1, k) + ")" + strPeptideMod.substring(l+1);
				//TODO: support terminal mods
				modPep = modPep.replace(/\[.+?\]/g, ""); // remove all other mods
				parser.addMod(strProtein, mod, modPep);
				k++;
			}
		}
	}
	
	this.parseIdList = function (lines) {
		for(var j = 0, line; line = lines[j]; j++) {
			var elems = line.split(/\s+/g);
			if (elems.length==0)
				continue;
			var strProtein = elems[0].split(/[,;]/g)[0]; // first of protein groups
			if (elems.length==1) {
				parser.ensureProt(strProtein);
				//addProtein(elems[0], null, [], {}, true);
				//addProtein(arrItems[0], null, arrPeps, {}, true);
			} else if (elems.length>=2) {
				var arrPeps = elems[1].split(/[,;]/g);
				for(var i = 0, strPeptideMod; strPeptideMod = arrPeps[i]; i++) {
					if (strPeptideMod.charAt(1)=='.') { // K.LLDEN[115.03]M[147.04]AR.I | K.n[58.03]N[115.03]GVN[115.03]GTGEN[115.03]GR.K 
						strPeptideMod = strPeptideMod.substring(2, strPeptideMod.length-2); // LLDEN[115.03]M[147.04]AR
					}
					var strPeptide = strPeptideMod.replace(/\[.+?\]/g,""); // remove all mods
					if (strPeptideMod.indexOf("n[")==0)
						strPeptide = strPeptide.substring(1);
					parser.addPeptide(strProtein, strPeptide, strPeptideMod.replace(/\[/g, "<span class='mod'>").replace(/\]/g, "</span>"));
					
					var k = 0;
					if (strPeptideMod.indexOf("n[")==0) 
						strPeptideMod = strPeptideMod.substring(1);
					// store all modified peptide sequences, per modification [cannot get the positions]
					while((k = strPeptideMod.indexOf('[', k)) >= 0) {
						var l = strPeptideMod.indexOf(']', k);
						var mod = (k==0 ? "n":"") + strPeptideMod.substring(k-1, l+1);
						var modPep = strPeptideMod.substring(0, k-1) + "(" + strPeptideMod.substring(k-1, k) + ")" + strPeptideMod.substring(l+1);
						if (k==0)
							modPep = "(" + strPeptideMod.substring(l+1, l+2) + ")" + strPeptideMod.substring(l+2); // n-terminal mods will be attributed to first AA
						//TODO: support cterminal mods
						modPep = modPep.replace(/\[.+?\]/g, ""); // remove all other mods
						parser.addMod(strProtein, mod, modPep);
						k++;
					}
				}
			}
		}
	};
	
	/*
	 * END - specific parse functions
	 */
	
	// start reading first file
	this.readFile(0);
};

