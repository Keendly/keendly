$ ->
  # init materialize components
  $(".button-collapse").sideNav()
  # try to guess user's timezone
  initTimezone()

  DELIVER_NOW_MODAL_BTN = $('#deliver_now')
  DELIVER_NOW_MODAL = $('#deliver_modal')
  DELIVER_NOW_FEED_LIST = $('#feed_list')
  DELIVER_NOW_DETAILED_FEED_LIST = $('#detailed_feed_list')

  SCHEDULE_DELIVERY_MODAL_BTN = $('#schedule_delivery')
  SCHEDULE_DELIVERY_MODAL = $('#schedule_modal')
  SCHEDULE_DELIVERY_FEED_LIST = $('#schedule_feed_list')
  SCHEDULE_DELIVERY_DETAILED_FEED_LIST = $('#detailed_schedule_feed_list')

  DELIVER_NOW_BTN = $('#deliver_button')
  DELIVER_NOW_FORM = $('#deliver_form')
  DELIVER_NOW_PROGRESS = $('#progress')
  DELIVER_NOW_SWITCH = $('#mode')

  SCHEDULE_DELIVERY_BTN = $('#schedule_button')
  SCHEDULE_DELIVERY_PROGRESS = $('#schedule_progress')
  SCHEDULE_DELIVERY_FORM = $('#schedule_form')
  SCHEDULE_DELIVERY_SWITCH = $('#schedule_mode')

  SELF_REMOVE_CLASS = '.self_remove'

  DELIVER_NOW_MODAL_BTN.click ->
    removeEmptyListPlaceholder()
    clearAndShow(DELIVER_NOW_FEED_LIST)
    fillWithSelectedFeeds(DELIVER_NOW_FEED_LIST, false)
    if isEmpty(DELIVER_NOW_FEED_LIST)
      alert('nothing to deliver')
    else
      DELIVER_NOW_MODAL.openModal();

  SCHEDULE_DELIVERY_MODAL_BTN.click ->
    removeEmptyListPlaceholder()
    clearAndShow(SCHEDULE_DELIVERY_FEED_LIST)
    fillWithSelectedFeeds(SCHEDULE_DELIVERY_FEED_LIST, false)
    if isEmpty(SCHEDULE_DELIVERY_FEED_LIST)
      alert('nothing to deliver')
    else
      SCHEDULE_DELIVERY_MODAL.openModal();

  DELIVER_NOW_BTN.click ->
    if DELIVER_NOW_BTN.hasClass('disabled')
      return
    DELIVER_NOW_PROGRESS.show()
    request = new Object()
    request.feeds = []
    if DELIVER_NOW_SWITCH.is(":checked")
      feeds = DELIVER_NOW_DETAILED_FEED_LIST.children()
      for i in [0...feeds.length]
        feed = feeds.eq(i).children().eq(0)
        feedRequest = requestFeed(
          feed.attr('feed_id'),
          feed.attr('title'),
          feed.find('.include_images').is(":checked"),
          feed.find('.mark_as_read').is(":checked"),
          feed.find('.full_article').is(":checked")
        )
        request.feeds.push(feedRequest)
    else
      includeImages = DELIVER_NOW_FORM.find('#include_images')
      markAsRead = DELIVER_NOW_FORM.find('#mark_as_read')
      fullArticle = DELIVER_NOW_FORM.find('#full_article')
      feeds = DELIVER_NOW_FEED_LIST.children()
      for i in [0...feeds.length]
        feed = feeds.eq(i).children().eq(0)
        feedRequest = requestFeed(
          feed.attr('feed_id'),
          feed.attr('title'),
          includeImages.is(":checked"),
          markAsRead.is(":checked"),
          fullArticle.is(":checked")
        )
        request.feeds.push(feedRequest)

    post(
      'deliver',
      request,
      DELIVER_NOW_PROGRESS,
      -> alert('success'),
      -> alert('error')
    )

  SCHEDULE_DELIVERY_BTN.click ->
    if SCHEDULE_DELIVERY_BTN.hasClass('disabled')
      return
    SCHEDULE_DELIVERY_PROGRESS.show()
    request = new Object()
    request.feeds = []
    if SCHEDULE_DELIVERY_SWITCH.is(":checked")
      feeds = SCHEDULE_DELIVERY_DETAILED_FEED_LIST.children()
      for i in [0...feeds.length]
        feed = feeds.eq(i).children().eq(0)
        feedRequest = requestFeed(
          feed.attr('feed_id'),
          feed.attr('title'),
          feed.find('.include_images').is(":checked"),
          feed.find('.full_article').is(":checked"),
          feed.find('.mark_as_read').is(":checked")
        )
        request.feeds.push(feedRequest)
    else
      includeImages = SCHEDULE_DELIVERY_FORM.find('#schedule_include_images')
      markAsRead = SCHEDULE_DELIVERY_FORM.find('#schedule_mark_as_read')
      fullArticle = SCHEDULE_DELIVERY_FORM.find('#schedule_full')
      feeds = SCHEDULE_DELIVERY_FEED_LIST.children()
      for i in [0...feeds.length]
        feed = feeds.eq(i).children().eq(0)
        feedRequest = requestFeed(
          feed.attr('feed_id'),
          feed.attr('title'),
          includeImages.is(":checked"),
          fullArticle.is(":checked"),
          markAsRead.is(":checked")
        )
        request.feeds.push(feedRequest)

    post(
      'schedule',
      request,
      SCHEDULE_DELIVERY_PROGRESS,
      -> alert('success'),
      -> alert('error')
    )

  # handler for mode switch on deliver now modal
  DELIVER_NOW_SWITCH.change ->
    removeEmptyListPlaceholder()
    enableButton(DELIVER_NOW_BTN)
    if DELIVER_NOW_SWITCH.is(':checked')
      clearAndShow(DELIVER_NOW_DETAILED_FEED_LIST)
      fillWithSelectedFeeds(DELIVER_NOW_DETAILED_FEED_LIST, true)
      $('#detailed').show()
      $('#simple').hide()
    else
      clearAndShow(DELIVER_NOW_FEED_LIST)
      fillWithSelectedFeeds(DELIVER_NOW_FEED_LIST, false)
      $('#detailed').hide()
      $('#simple').show()

  # handler for mode switch on schedule delivery modal
  SCHEDULE_DELIVERY_SWITCH.change ->
    removeEmptyListPlaceholder()
    enableButton(SCHEDULE_DELIVERY_BTN)
    if SCHEDULE_DELIVERY_SWITCH.is(':checked')
      clearAndShow(SCHEDULE_DELIVERY_DETAILED_FEED_LIST)
      fillWithSelectedFeeds(SCHEDULE_DELIVERY_DETAILED_FEED_LIST, true)
      $('#schedule_detailed').show()
      $('#schedule_simple').hide()
    else
      clearAndShow(SCHEDULE_DELIVERY_FEED_LIST)
      fillWithSelectedFeeds(SCHEDULE_DELIVERY_FEED_LIST, false)
      $('#schedule_detailed').hide()
      $('#schedule_simple').show()

  # live handler for feed list remove button
  $(document).on 'click', SELF_REMOVE_CLASS, (event) ->
    list = $(@).parent().parent().parent()
    if list.children().length == 1
      list.empty()
      list.parent().append(emptyListPlaceholder)
      list.hide()
      btn = list.parent().parent().parent().find('.submit')
      btn.addClass('disabled')
      # todo disable deliver/schedule button
    $(@).parent().parent().remove()

  $("#search_box").keyup ->
    filter = $(this).val()
    subscriptions = $('#subscriptions').find('tr')
    subscriptionsLength = subscriptions.length
    for i in [0...subscriptionsLength]
      subscription = subscriptions.eq(i)
      columns = subscription.find('td')
      if columns.length > 0
        text = columns.eq(1).text()
        if text.search(new RegExp(filter, "i")) < 0
          subscription.hide()
        else
          subscription.show();

post = (url, request, progressbar, success_callback, error_callback) ->
  $.ajax
    url: url,
    type: 'POST',
    data: JSON.stringify(request),
    contentType: 'application/json; charset=utf-8',
    dataType: 'json',
    success: ->
      progressbar.hide()
      success_callback()
    error: ->
      progressbar.hide()
      error_callback()

requestFeed = (feed_id, title, includeImages, markAsRead, fullArticle) ->
  request = new Object()
  request.id = feed_id
  request.title = title
  request.includeImages = includeImages
  request.fullArticle = fullArticle
  request.markAsRead = markAsRead
  request

fillWithSelectedFeeds = (elementToFill, detailed) ->
  subscriptions = $('#subscriptions').find('tr')
  subscriptionsLength = subscriptions.length
  for i in [0...subscriptionsLength]
    subscription = subscriptions.eq(i)
    columns = subscription.find('td')
    if columns.length > 0
      checkbox = columns.eq(0).find('.filled-in')
      if checkbox.is(':checked')
        feed_id = checkbox.attr('name')
        title = columns.eq(1).text()
        if detailed
          elementToFill.append(detailedFeedListItem(feed_id, title, i))
        else
          elementToFill.append(simpleFeedListItem(feed_id, title))

simpleFeedListItem = (feed_id, title) ->
    "<li class='collection-item'>
    <div feed_id='#{feed_id}' title='#{title}'>
    #{title}
    <a href='#!' class='secondary-content self_remove'>
        <i class='material-icons'>clear</i>
    </a>
    </div></li>"

detailedFeedListItem = (feed_id, title, i) ->
    "<li class='collection-item'>
    <div feed_id='#{feed_id}' title='#{title}'>
        #{title}
        <a href='#!' class='secondary-content self_remove'>
            <i class='material-icons'>clear</i>
        </a>
        <p>
          <input type='checkbox' class='filled-in include_images' id='include_images#{i}'/>
          <label for='include_images#{i}'>Include images</label>
          </p>
          <p>
          <input type='checkbox' class='filled-in mark_as_read' id='mark_as_read#{i}' checked='checked'/>
          <label for='mark_as_read#{i}'>Mark as read</label>
          </p>
          <p>
          <input type='checkbox' class='filled-in full_article' id='full_article#{i}' checked='checked'/>
          <label for='full_article#{i}'>Full article</label>
        </p>
        </div></li>"

clearAndShow = (elem) ->
  elem.empty()
  elem.show()

emptyListPlaceholder = ->
  "<div class='empty_div'>No feeds selected</div>"

removeEmptyListPlaceholder = ->
  $('.empty_div').remove()

enableButton = (button) ->
  button.removeClass('disabled')

initTimezone = ->
  tz = jstz.determine();
  timezones = $('#timezones').find('option')
  for i in [0...timezones.length]
    timezone = timezones[i]
    if timezone.value == tz.name()
        timezone.selected = "selected"

isEmpty = (elem) ->
  elem.children().length == 0
