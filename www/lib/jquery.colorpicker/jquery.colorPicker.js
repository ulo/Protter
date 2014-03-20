/**
 * Really Simple Color Picker in jQuery
 * 
 * Copyright (c) 2008 Lakshan Perera (www.laktek.com)
 * Licensed under the MIT (MIT-LICENSE.txt)  licenses.
 * 
 */

(function($){
  $.fn.colorPicker = function(allowNoChange){    
    if(this.length > 0) buildSelector();
    return this.each(function(i) {
      buildPicker(this,allowNoChange)}); 
  };
  
  var selectorOwner;
  var selectorShowing = false;
  
  buildPicker = function(element, allowNoChange){
    //build color picker
    control = $("<div class='color_picker ui-state-default ui-corner-all' ><span class='ui-icon ui-icon-cancel'></span>&nbsp;</div>")
	
	if (allowNoChange)
		control.addClass("colorPickerAllowNoChange");
	
	if (toHex($(element).val())) {
		control.css('background-color', toHex($(element).val()));
		control.prop("title",$(element).val()); // UO: added tooltip
		$("span", control).css("display","none");
	} else {
		if (allowNoChange) {
			control.prop("title","do not change color"); // UO: added tooltip
			$("span", control).css("display","inline-block");
		} else
			$("span", control).css("display","none"); 
	}
    
    //bind click event to color picker
    control.bind("click", toggleSelector);
    
    //add the color picker section
    $(element).after(control);

    //add event listener to input box
    $(element).bind("change", function() {
      selectedValue = toHex($(element).val());
      $(element).next(".color_picker").css("background-color", selectedValue);
    });
    
    //UO:
    $(element).bind("refresh", function() {
      selectedValue = toHex($(element).val());
      $(element).next(".color_picker").css("background-color", selectedValue);
    });
    
    //hide the input box
    $(element).hide();

  };
  
  buildSelector = function(){
    selector = $("<div id='color_selector' class='ui-widget ui-state-default ui-corner-all'></div>");

	noColorSwatch = $("<div title='no change in color' class='color_swatch color_swatch_noChange'><span class='ui-icon ui-icon-cancel'></span>no change</div>");
	noColorSwatch.bind("click", function(e){ changeColor() });
	selector.append(noColorSwatch);
	
     //add color pallete
     $.each($.fn.colorPicker.svgColors, function(svgName, hexCode){
      swatch = $("<div class='color_swatch' title='"+svgName+"' >&nbsp;</div>")
      swatch.css("background-color", hexCode);
      swatch.bind("click", function(e){ changeColor(svgName) });
      swatch.appendTo(selector);
     });
     
     $("body").append(selector); 
     selector.hide();
  };
  
  checkMouse = function(event){
    //check the click was on selector itself or on selectorOwner
    var selector = "div#color_selector";
    var selectorParent = $(event.target).parents(selector).length;
    if(event.target == $(selector)[0] || event.target == selectorOwner || selectorParent > 0) return
    
    hideSelector();   
  }
  
  hideSelector = function(){
    var selector = $("div#color_selector");
    
    $(document).unbind("mousedown", checkMouse);
    selector.hide();
    selectorShowing = false
  }
  
  showSelector = function(){
  	if ($(selectorOwner).hasClass("colorPickerAllowNoChange"))
		$(".color_swatch_noChange").show();
	else 
		$(".color_swatch_noChange").hide();
	
    var selector = $("div#color_selector");
    selector.css({
      top: $(selectorOwner).offset().top, //UO: moved from bottom to right
      left: $(selectorOwner).offset().left + ($(selectorOwner).outerWidth())
    }); 
    hexColor = $(selectorOwner).prev("input").val();
    selector.show();
    
    //bind close event handler
    $(document).bind("mousedown", checkMouse);
    selectorShowing = true 
   }
  
  toggleSelector = function(event){
    selectorOwner = this; 
    selectorShowing ? hideSelector() : showSelector();
  }
  
  changeColor = function(svgName){
    if(hexColor = toHex(svgName)){
      $(selectorOwner).css("background-color", hexColor);
      $(selectorOwner).prop("title",svgName); // UO: added tooltip
      $(selectorOwner).prev("input").val(svgName).change();
	  $("span",$(selectorOwner)).css("display","none");
      //close the selector
      hideSelector();
    } else {
		$(selectorOwner).css("background-color", "");
		$(selectorOwner).prop("title","do not change color"); // UO: added tooltip
		$(selectorOwner).prev("input").val("").change();
		$("span",$(selectorOwner)).css("display","inline-block");
		hideSelector();
	} 
  };
  
  toHex = function(colorName){
  	return $.fn.colorPicker.svgColors[colorName];
  }
  
  $.fn.colorPicker.svgColors = {
  		black : '#000000',
		dimgray : '#696969',
		gray : '#808080',
		darkgray : '#a9a9a9',
		silver : '#c0c0c0',
		lightgray : '#d3d3d3',
		gainsboro : '#dcdcdc',
		whitesmoke : '#f5f5f5',
		white : '#ffffff',
		rosybrown : '#bc8f8f',
		indianred : '#cd5c5c',
		brown : '#a52a2a',
		firebrick : '#b22222',
		lightcoral : '#f08080',
		maroon : '#800000',
		darkred : '#8b0000',
		red : '#ff0000',
		snow : '#fffafa',
		salmon : '#fa8072',
		mistyrose : '#ffe4e1',
		tomato : '#ff6347',
		darksalmon : '#e9967a',
		orangered : '#ff4500',
		coral : '#ff7f50',
		lightsalmon : '#ffa07a',
		sienna : '#a0522d',
		chocolate : '#d2691e',
		saddlebrown : '#8b4513',
		seashell : '#fff5ee',
		sandybrown : '#f4a460',
		peachpuff : '#ffdab9',
		peru : '#cd853f',
		linen : '#faf0e6',
		darkorange : '#ff8c00',
		bisque : '#ffe4c4',
		tan : '#d2b48c',
		burlywood : '#deb887',
		antiquewhite : '#faebd7',
		navajowhite : '#ffdead',
		blanchedalmond : '#ffebcd',
		papayawhip : '#ffefd5',
		moccasin : '#ffe4b5',
		wheat : '#f5deb3',
		oldlace : '#fdf5e6',
		orange : '#ffa500',
		floralwhite : '#fffaf0',
		goldenrod : '#daa520',
		darkgoldenrod : '#b8860b',
		cornsilk : '#fff8dc',
		gold : '#ffd700',
		khaki : '#f0e68c',
		lemonchiffon : '#fffacd',
		palegoldenrod : '#eee8aa',
		darkkhaki : '#bdb76b',
		beige : '#f5f5dc',
		lightgoldenrodyellow : '#fafad2',
		olive : '#808000',
		yellow : '#ffff00',
		lightyellow : '#ffffe0',
		ivory : '#fffff0',
		olivedrab : '#6b8e23',
		yellowgreen : '#9acd32',
		darkolivegreen : '#556b2f',
		greenyellow : '#adff2f',
		lawngreen : '#7cfc00',
		chartreuse : '#7fff00',
		darkseagreen : '#8fbc8f',
		forestgreen : '#228b22',
		limegreen : '#32cd32',
		lightgreen : '#90ee90',
		palegreen : '#98fb98',
		darkgreen : '#006400',
		green : '#008000',
		lime : '#00ff00',
		honeydew : '#f0fff0',
		seagreen : '#2e8b57',
		mediumseagreen : '#3cb371',
		springgreen : '#00ff7f',
		mintcream : '#f5fffa',
		mediumspringgreen : '#00fa9a',
		mediumaquamarine : '#66cdaa',
		aquamarine : '#7fffd4',
		turquoise : '#40e0d0',
		lightseagreen : '#20b2aa',
		mediumturquoise : '#48d1cc',
		darkslategray : '#2f4f4f',
		paleturquoise : '#afeeee',
		teal : '#008080',
		darkcyan : '#008b8b',
		aqua : '#00ffff',
		cyan : '#00ffff',
		lightcyan : '#e0ffff',
		azure : '#f0ffff',
		darkturquoise : '#00ced1',
		cadetblue : '#5f9ea0',
		powderblue : '#b0e0e6',
		lightblue : '#add8e6',
		deepskyblue : '#00bfff',
		skyblue : '#87ceeb',
		lightskyblue : '#87cefa',
		steelblue : '#4682b4',
		aliceblue : '#f0f8ff',
		slategray : '#708090',
		lightslategray : '#778899',
		dodgerblue : '#1e90ff',
		lightsteelblue : '#b0c4de',
		cornflowerblue : '#6495ed',
		royalblue : '#4169e1',
		midnightblue : '#191970',
		lavender : '#e6e6fa',
		navy : '#000080',
		darkblue : '#00008b',
		mediumblue : '#0000cd',
		blue : '#0000ff',
		ghostwhite : '#f8f8ff',
		darkslateblue : '#483d8b',
		slateblue : '#6a5acd',
		mediumslateblue : '#7b68ee',
		mediumpurple : '#9370db',
		blueviolet : '#8a2be2',
		indigo : '#4b0082',
		darkorchid : '#9932cc',
		darkviolet : '#9400d3',
		mediumorchid : '#ba55d3',
		thistle : '#d8bfd8',
		plum : '#dda0dd',
		violet : '#ee82ee',
		purple : '#800080',
		darkmagenta : '#8b008b',
		fuchsia : '#ff00ff',
		magenta : '#ff00ff',
		orchid : '#da70d6',
		mediumvioletred : '#c71585',
		deeppink : '#ff1493',
		hotpink : '#ff69b4',
		palevioletred : '#db7093',
		lavenderblush : '#fff0f5',
		crimson : '#dc143c',
		pink : '#ffc0cb',
		lightpink : '#ffb6c1'
  };
})(jQuery);


