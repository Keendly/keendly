$ ->
  deliveries = $('#deliveries').find('tr')
  if deliveries.length == 1
    $('#deliveries').parent().append(emptyListPlaceholder)

emptyListPlaceholder = ->
  "<div class='info_div'>No deliveries found</div>"
