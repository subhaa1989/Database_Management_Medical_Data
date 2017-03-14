(function(cs) {
	window.GATE = (window.GATE || {});
	window.GATE.Dicom = (window.GATE.Dicom || {});

    "use strict";
    var DICOMARRAY = [ {key: "4711" ,value: "sdasdasd"}];	
        
    
     window.GATE.Dicom.str2ab = function(str) {
        var buf = new ArrayBuffer(str.length*2); // 2 bytes for each char
        var bufView = new Uint16Array(buf);
        var index = 0;
        for (var i=0, strLen=str.length; i<strLen; i+=2) {
            var lower = str.charCodeAt(i);
            var upper = str.charCodeAt(i+1);
            bufView[index] = lower + (upper <<8);
            index++;
        }
        return bufView;
    }

    
    window.GATE.Dicom.getPixelData = function(base64PixelData)
    {
        var pixelDataAsString = window.atob(base64PixelData);
        var pixelData = window.GATE.Dicom.str2ab(pixelDataAsString);
        return pixelData;
    }
    
    
    window.GATE.Dicom.GetArrayValuebyKey = function(type, currentArr) {
		var result = $(currentArr).filter(function(v) {
   		 return currentArr[v].key === type; // Filter out the appropriate one
		})[0];
		
		if (result != null && result != undefined){
			return result.value;
		}
		else
			return null
	}
    
    
    
    
    

    window.GATE.Dicom.getImage = function(imageId) {    
         var width = 256;
         var height = 256;

        function getPixelData(){

            if(imageId != undefined){

                var storedDicom = window.GATE.Dicom.GetArrayValuebyKey(imageId ,DICOMARRAY);

                if(storedDicom != null){
                    return window.GATE.Dicom.getPixelData(storedDicom);
                }
                else{
                   storedDicom = window.GATE.Overview.GetFile(imageId);
                   DICOMARRAY.push({key : imageId , value : storedDicom});

                   return window.GATE.Dicom.getPixelData(storedDicom);
                }
            }
            else{
                throw "unknown imageId";
            }
        }
            var image = {
            imageId: imageId,
            minPixelValue : 0,
            maxPixelValue : 257,
            slope: 1.0,
            intercept: 0,
            windowCenter : 127,
            windowWidth : 256,
            render: cs.renderGrayscaleImage,
            getPixelData: getPixelData,
            rows: height,
            columns: width,
            height: height,
            width: width,
            color: false,
            columnPixelSpacing: .8984375,
            rowPixelSpacing: .8984375,
            sizeInBytes: width * height * 2
            };

        var deferred = $.Deferred();
        deferred.resolve(image);
        return deferred;  
     }
    
    
    window.GATE.Dicom.BlobToFile = function(blob){
        var filename = "tempFile" + count.toString();
        blob.lastModifiedDate = new Date();
        blob.name = filename;
        return blob;
    }   

   
    window.GATE.Dicom.Initialize = function(){
    // register our imageLoader plugin with cornerstone
    cs.registerImageLoader('', window.GATE.Dicom.getImage);
    }
    
    
    window.GATE.Dicom.LoadImage = function(imageId){
     
    var element = $('#dicomImage').get(0);
    if( imageId === null) return; 
    window.GATE.Overview.ToggleCustomModal();
    $('#dicomImage').on("CornerstoneImageRendered", window.GATE.Dicom.onViewportUpdated);

    var config = {
        // invert: true,
        minScale: 0.25,
        maxScale: 20.0,
        preventZoomOutsideImage: true
    };
    cornerstoneTools.zoom.setConfiguration(config);

    $('#chkshadow').on('change', function(){
      cornerstoneTools.length.setConfiguration({shadow: this.checked});
      cornerstoneTools.angle.setConfiguration({shadow: this.checked});
      cornerstone.updateImage(element);
    });

    // image enable the dicomImage element
    cornerstone.enable(element);
    cornerstone.loadImage(imageId).then(function(image) {
        cornerstone.displayImage(element, image);
        cornerstoneTools.mouseInput.enable(element);
        cornerstoneTools.mouseWheelInput.enable(element);
        // Enable all tools we want to use with this element
        cornerstoneTools.wwwc.activate(element, 1); // ww/wc is the default tool for left mouse button
        cornerstoneTools.pan.activate(element, 2); // pan is the default tool for middle mouse button
        cornerstoneTools.zoom.activate(element, 4); // zoom is the default tool for right mouse button
        cornerstoneTools.zoomWheel.activate(element); // zoom is the default tool for middle mouse wheel
        cornerstoneTools.probe.enable(element);
        cornerstoneTools.length.enable(element);
        cornerstoneTools.ellipticalRoi.enable(element);
        cornerstoneTools.rectangleRoi.enable(element);
        cornerstoneTools.angle.enable(element);
        cornerstoneTools.highlight.enable(element);

        activate("#enableWindowLevelTool");

        function activate(id)
        {
            $('a').removeClass('active');
            $(id).addClass('active');
        }
        // helper function used by the tool button handlers to disable the active tool
        // before making a new tool active
        function disableAllTools()
        {
            cornerstoneTools.wwwc.disable(element);
            cornerstoneTools.pan.activate(element, 2); // 2 is middle mouse button
            cornerstoneTools.zoom.activate(element, 4); // 4 is right mouse button
            cornerstoneTools.probe.deactivate(element, 1);
            cornerstoneTools.length.deactivate(element, 1);
            cornerstoneTools.ellipticalRoi.deactivate(element, 1);
            cornerstoneTools.rectangleRoi.deactivate(element, 1);
            cornerstoneTools.angle.deactivate(element, 1);
            cornerstoneTools.highlight.deactivate(element, 1);
            cornerstoneTools.freehand.deactivate(element, 1);
        }

        // Tool button event handlers that set the new active tool
        $('#enableWindowLevelTool').click(function() {
            activate('#enableWindowLevelTool')
            disableAllTools();
            cornerstoneTools.wwwc.activate(element, 1);
        });
        $('#pan').click(function() {
            activate('#pan')
            disableAllTools();
            cornerstoneTools.pan.activate(element, 3); // 3 means left mouse button and middle mouse button
        });
        $('#zoom').click(function() {
            activate('#zoom')
            disableAllTools();
            cornerstoneTools.zoom.activate(element, 5); // 5 means left mouse button and right mouse button
        });
        $('#enableLength').click(function() {
            activate('#enableLength')
            disableAllTools();
            cornerstoneTools.length.activate(element, 1);
        });
        $('#probe').click(function() {
            activate('#probe')
            disableAllTools();
            cornerstoneTools.probe.activate(element, 1);
        });
        $('#circleroi').click(function() {
            activate('#circleroi')
            disableAllTools();
            cornerstoneTools.ellipticalRoi.activate(element, 1);
        });
        $('#rectangleroi').click(function() {
            activate('#rectangleroi')
            disableAllTools();
            cornerstoneTools.rectangleRoi.activate(element, 1);
        });
        $('#angle').click(function () {
            activate('#angle')
            disableAllTools();
            cornerstoneTools.angle.activate(element, 1);
        });
        $('#highlight').click(function() {
            activate('#highlight')
            disableAllTools();
            cornerstoneTools.highlight.activate(element, 1);
        });
        $('#freehand').click(function() {
            activate('#freehand')
            disableAllTools();
            cornerstoneTools.freehand.activate(element, 1);
        });
    });
    
    
    }
    
    // Listen for changes to the viewport so we can update the text overlays in the corner
    window.GATE.Dicom.onViewportUpdated = function(e) {
        var viewport = cornerstone.getViewport(e.target)
        $('#mrbottomleft').text("WW/WC: " + Math.round(viewport.voi.windowWidth) + "/" + Math.round(viewport.voi.windowCenter));
        $('#mrbottomright').text("Zoom: " + viewport.scale.toFixed(2));
    };
	

})(cornerstone);

$(document).ready(function() {
    window.GATE.Dicom.Initialize();
});