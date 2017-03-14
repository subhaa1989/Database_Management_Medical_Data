(function() {
	window.GATE = (window.GATE || {});
	window.GATE.Helper = (window.GATE.Helper || {});
	
    var accentMap = {
      "á": "a",
      "ö": "o",
	  	"ä": "a",
	  	"ü": "u"
    };
	
	window.GATE.Helper.Initialize = function(){
      var autoComplete = $("GD-Item-FunctionScoreName");
		
     if(autoComplete != null){
        autoComplete.autocomplete({
        source: function( request, response ) {
          var matcher = new RegExp( $.ui.autocomplete.escapeRegex( request.term ), "i" );
          response( $.grep( fsNames, function( value ) {
          value = value.label || value.value || value;
          return matcher.test( value ) || matcher.test( window.GATE.Helper.normalize( value ) );
          }) );
        }
        });
     }
	}
	
  window.GATE.Helper.normalize = function( term ) {
      var ret = "";
      for ( var i = 0; i < term.length; i++ ) {
        ret += accentMap[ term.charAt(i) ] || term.charAt(i);
      }
      return ret;
    };
	
	window.GATE.Helper.attachAutoComplition = function(request, response , values){
        var matcher = new RegExp( $.ui.autocomplete.escapeRegex( request.term ), "i" );
        response( $.grep( values, function( value ) {
          value = value.label || value.value || value;
          return matcher.test( value ) || matcher.test( window.GATE.Helper.normalize( value ) );
        }));
  }
	
  window.GATE.Helper.DeleteConfirmation = function(dfd, targ){
    var modalConfirm = $('<div/>', {
                        id: 'dialog-confirm',
                        class: 'gd-dialog-confirm',
                        title: 'Confirm Deletion'
                       }).append($('<div/>',{
                          text: 'Do you want to delete this item? This item will be removed permanently and cannot be recovered.'
                                }));
    
    $('body').append(modalConfirm);
    
    $('#dialog-confirm').dialog({
      resizable: false,
      width: 400,
      modal: true,
      buttons:{
          DISAGREE: function(){
            dfd.reject(targ);         
            $(this).dialog("close");
            modalConfirm.remove();
          },
          AGREE: function(){
            dfd.resolve(targ);
            $(this).dialog("close");
            modalConfirm.remove();
          }
      }
    })
    
    return dfd; 
  }

})(jQuery);

$(document).ready(function() {
	window.GATE.Helper.Initialize();
	
});