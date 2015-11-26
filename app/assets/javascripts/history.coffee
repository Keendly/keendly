$ ->
  deliveries = $('#deliveries').find('tr')
  if deliveries.length == 1
    $('#deliveries').after(emptyListPlaceholder)

emptyListPlaceholder = ->
  "<div class='info_div'>No deliveries found</div>"
