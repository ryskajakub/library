$(document).ready(function(){
	$("form").each(function(){
		$(this).find(".lock").attr("disabled","disabled");
		var fields = $(this).find(".min1");
		var selectFields = $(this).find(".option1");
		var emailFields = $(this).find(".email");
		var validateThem = function(){
			var allValuesAreValid = true;
			fields.each(function(){
				if(($(this).val().length < 1))
					allValuesAreValid = false;
			});
			selectFields.each(function(){
				var numselected = 0;
				$(this).find("option:selected").each(function(){
					numselected++;
				});
				if (numselected < 1){
					allValuesAreValid = false;
				}
			});
			emailFields.each(function(){
				if (($(this).val().match(/^[^@]+@\w+\.[a-z]{2,}$/) == null)){
					allValuesAreValid = false;
				}
			});
			return allValuesAreValid;
		};
		var formScope = $(this);
		var handler = function(){
			if (validateThem()) {
				formScope.find(".lock").removeAttr("disabled");
			} else {
				formScope.find(".lock").attr("disabled","disabled");
			}
		};
		$(this).find("div.validate .email").each(function(){
			$(this).bind("paste",function(){setTimeout(handler,100)});
			$(this).keyup(handler);
		});
		$(this).find("div.validate .min1").each(function(){
			$(this).bind("paste",function(){setTimeout(handler,100)});
			$(this).keyup(handler);
		});
		$(this).find("div.validate .option1").each(function(){
			$(this).change(handler);
		});
	})
});
