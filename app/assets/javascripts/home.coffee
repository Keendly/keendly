SUBSCRIPTION_FEED_LIST_SELECTOR = '#subscription_feed_list'
SUBSCRIPTION_DETAILED_FEED_LIST_SELECTOR = '#detailed_subscription_feed_list'
SUBSCRIPTION_MODE_SELECTOR = '#subscription_mode'

SUBSCRIPTION_EDIT_FEED_LIST_SELECTOR = '#subscription_edit_feed_list'
SUBSCRIPTION_EDIT_DETAILED_FEED_LIST_SELECTOR = '#detailed_subscription_edit_feed_list'
SUBSCRIPTION_EDIT_MODE_SELECTOR = '#subscription_edit_mode'

DELIVERY_FEED_LIST_SELECTOR = '#feed_list';
DELIVERY_DETAILED_FEED_LIST_SELECTOR = '#detailed_feed_list'
$ ->
  DELIVERY_MODAL_BTN = $('#delivery_modal_btn')
  DELIVERY_MODAL = $('#delivery_modal')
  DELIVERY_FEED_LIST = $(DELIVERY_FEED_LIST_SELECTOR)
  DELIVERY_DETAILED_FEED_LIST = $(DELIVERY_DETAILED_FEED_LIST_SELECTOR)

  SUBSCRIPTION_MODAL_BTN = $('#subscription_modal_btn')
  SUBSCRIPTION_MODAL = $('#subscription_modal')
  SUBSCRIPTION_FEED_LIST = $(SUBSCRIPTION_FEED_LIST_SELECTOR)
  SUBSCRIPTION_DETAILED_FEED_LIST = $(SUBSCRIPTION_DETAILED_FEED_LIST_SELECTOR)

  DELIVERY_SAVE_BTN = $('#delivery_save_btn')
  DELIVERY_FORM = $('#delivery_form')
  DELIVERY_PROGRESS = $('#delivery_progress')
  DELIVERY_MODE = $('#delivery_mode')

  SUBSCRIPTION_EDIT_MODAL = $('#subscription_edit_modal')
  SUBSCRIPTION_EDIT_FEED_LIST = $(SUBSCRIPTION_EDIT_FEED_LIST_SELECTOR)
  SUBSCRIPTION_EDIT_DETAILED_FEED_LIST = $(SUBSCRIPTION_EDIT_DETAILED_FEED_LIST_SELECTOR)

  SUBSCRIPTION_SAVE_BTN = $('#subscription_save_btn')
  SUBSCRIPTION_UPDATE_BTN = $('#subscription_update_btn')
  SUBSCRIPTION_DELETE_BTN = $('#subscription_delete_btn')
  SUBSCRIPTION_PROGRESS = $('#subscription_progress')
  SUBSCRIPTION_FORM = $('#subscription_form')
  SUBSCRIPTION_MODE = $(SUBSCRIPTION_MODE_SELECTOR)
  SUBSCRIPTION_EDIT_MODE = $('#subscription_edit_mode')

  SELF_REMOVE_CLASS = '.self_remove'
  SUBSCRIPTION_EDIT = $('.schedule_edit')

  # try to guess user's timezone
  initTimezone()

  DELIVERY_MODAL_BTN.click ->
    removeEmptyListPlaceholder()
    enableButton(DELIVERY_SAVE_BTN)
    clearAndShow(DELIVERY_FEED_LIST)

    DELIVERY_MODE.attr('checked',false)
    showSelectedFeedList(DELIVERY_FEED_LIST, DELIVERY_DETAILED_FEED_LIST, false)
    if isEmpty(DELIVERY_FEED_LIST)
      alert('nothing to deliver')
    else
      DELIVERY_MODAL.openModal();

  SUBSCRIPTION_MODAL_BTN.click ->
    removeEmptyListPlaceholder()
    enableButton(SUBSCRIPTION_SAVE_BTN)
    clearAndShow(SUBSCRIPTION_FEED_LIST)

    SUBSCRIPTION_MODE.attr('checked',false);
    showSelectedFeedList(SUBSCRIPTION_FEED_LIST, SUBSCRIPTION_DETAILED_FEED_LIST, false)
    if isEmpty(SUBSCRIPTION_FEED_LIST)
      alert('nothing to deliver')
    else
      SUBSCRIPTION_MODAL.openModal();

  SUBSCRIPTION_EDIT.click ->
    removeEmptyListPlaceholder()
    enableButton(SUBSCRIPTION_UPDATE_BTN)
    enableButton(SUBSCRIPTION_DELETE_BTN)
    subscription_id = $(@).attr('sid')
    $.ajax
      url: 'subscription?id=' + subscription_id,
      type: 'GET',
      dataType: 'json',
      success: (data) ->
        fillModalWithSubscription(data)
        SUBSCRIPTION_EDIT_MODAL.openModal();
      error: ->
        alert('error!')

  DELIVERY_SAVE_BTN.click ->
    if DELIVERY_SAVE_BTN.hasClass('disabled')
      return
    DELIVERY_PROGRESS.show()
    request = new Object()
    request.feeds = []
    if DELIVERY_MODE.is(":checked")
      feeds = DELIVERY_DETAILED_FEED_LIST.children()
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
      includeImages = DELIVERY_FORM.find('#include_images')
      markAsRead = DELIVERY_FORM.find('#mark_as_read')
      fullArticle = DELIVERY_FORM.find('#full_article')
      feeds = DELIVERY_FEED_LIST.children()
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
      DELIVERY_PROGRESS,
      -> alert('success'),
      -> alert('error')
    )

  SUBSCRIPTION_SAVE_BTN.click ->
    if SUBSCRIPTION_SAVE_BTN.hasClass('disabled')
      return
    SUBSCRIPTION_PROGRESS.show()
    request = new Object()
    request.feeds = []
    if SUBSCRIPTION_MODE.is(":checked")
      feeds = SUBSCRIPTION_DETAILED_FEED_LIST.children()
      for i in [0...feeds.length]
        feed = feeds.eq(i).children().eq(0)
        feedRequest = requestFeed(
          feed.attr('feed_id'),
          feed.attr('title'),
          feed.find('.include_images').is(":checked"),
          feed.find('.full_article').is(":checked"),
          feed.find('.mark_as_read').is(":checked")
        )
        request.time = SUBSCRIPTION_FORM.find('#times_detailed').val()
        request.timezone = SUBSCRIPTION_FORM.find('#timezones_detailed').val()
        request.feeds.push(feedRequest)
    else
      includeImages = SUBSCRIPTION_FORM.find('#schedule_include_images')
      markAsRead = SUBSCRIPTION_FORM.find('#schedule_mark_as_read')
      fullArticle = SUBSCRIPTION_FORM.find('#schedule_full')
      feeds = SUBSCRIPTION_FEED_LIST.children()
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
      request.time = SUBSCRIPTION_FORM.find('#times_simple').val()
      request.timezone = SUBSCRIPTION_FORM.find('#timezones_simple').val()
    post(
      'schedule',
      request,
      SUBSCRIPTION_PROGRESS,
      -> alert('success'),
      -> alert('error')
    )

  SUBSCRIPTION_UPDATE_BTN.click ->
    if SUBSCRIPTION_UPDATE_BTN.hasClass('disabled')
      return
    alert('not implemented')

  SUBSCRIPTION_DELETE_BTN.click ->
    if SUBSCRIPTION_DELETE_BTN.hasClass('disabled')
      return
    alert('not implemented')

  # handler for mode switch on deliver now modal
  DELIVERY_MODE.change ->
    removeEmptyListPlaceholder()
    enableButton(DELIVERY_SAVE_BTN)
    if DELIVERY_MODE.is(':checked')
      showSelectedFeedList(DELIVERY_DETAILED_FEED_LIST, DELIVERY_FEED_LIST, true)
    else
      showSelectedFeedList(DELIVERY_FEED_LIST, DELIVERY_DETAILED_FEED_LIST, false)

  # handler for mode switch on schedule delivery modal
  SUBSCRIPTION_MODE.change ->
    removeEmptyListPlaceholder()
    if SUBSCRIPTION_MODE.is(':checked')
      showSelectedFeedList(SUBSCRIPTION_DETAILED_FEED_LIST, SUBSCRIPTION_FEED_LIST, true)
    else
      showSelectedFeedList(SUBSCRIPTION_FEED_LIST, SUBSCRIPTION_DETAILED_FEED_LIST, false)

  # handler for mode switch on subscription edit modal
  SUBSCRIPTION_EDIT_MODE.change ->
    if SUBSCRIPTION_EDIT_MODE.is(':checked')
      subscription = recoverSubscriptionFromModal(false)
      fillModalWithSubscriptionFixed(subscription, true)
    else
      subscription = recoverSubscriptionFromModal(true)
      fillModalWithSubscriptionFixed(subscription, false)

  # live handler for feed list remove button
  $(document).on 'click', SELF_REMOVE_CLASS, (event) ->
    list = $(@).parent().parent().parent()
    if list.children().length == 1
      list.empty()
      list.parent().append(emptyListPlaceholder)
      list.hide()
      btn = list.parent().parent().parent().find('.save')
      if btn != null
        btn.addClass('disabled')
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

fillModalWithSubscriptionFixed = (subscription, detailed) ->
  subscriptionMode = $(SUBSCRIPTION_EDIT_MODE_SELECTOR)
  subscriptionFeedList = $(SUBSCRIPTION_EDIT_FEED_LIST_SELECTOR)
  subscriptionDetailedFeedList = $(SUBSCRIPTION_EDIT_DETAILED_FEED_LIST_SELECTOR)

  fillModalWithSubscriptionTime(subscription)

  if (detailed)
    showSubscriptionFeedList(subscription, subscriptionDetailedFeedList, subscriptionFeedList, true)
  else
    showSubscriptionFeedList(subscription, subscriptionFeedList, subscriptionDetailedFeedList, false)

fillModalWithSubscription = (subscription) ->
  subscriptionMode = $(SUBSCRIPTION_EDIT_MODE_SELECTOR)
  subscriptionFeedList = $(SUBSCRIPTION_EDIT_FEED_LIST_SELECTOR)
  subscriptionDetailedFeedList = $(SUBSCRIPTION_EDIT_DETAILED_FEED_LIST_SELECTOR)

  fillModalWithSubscriptionTime(subscription)

  if isSimple(subscription)
    subscriptionMode.prop('checked', false)
    showSubscriptionFeedList(subscription, subscriptionFeedList, subscriptionDetailedFeedList, false)
  else
    subscriptionMode.prop('checked', true)
    showSubscriptionFeedList(subscription, subscriptionDetailedFeedList, subscriptionFeedList, true)

fillModalWithSubscriptionTime = (subscription) ->
  setTime(subscription['time'])
  setTimezone( subscription['timezone'])

isSimple = (subscription) ->
  # caluclate if all feeds have the same configuration
  simple = false
  if subscription['feeds'].length < 2
    simple = true
  else
    last = null
    for i in [0...subscription['feeds'].length]
      item = subscription['feeds'][i]
      if last != null
        if (last['withImages'] != item['withImages'] or
            last['fullArticle'] != item['fullArticle'] or
            last['markAsRead'] != item['markAsRead'])
          simple = false
          break
      last = item
    return simple

findFeedTitle = (feed_to_find) ->
  subscriptions = $('#subscriptions').find('tr')
  subscriptionsLength = subscriptions.length
  for i in [0...subscriptionsLength]
    subscription = subscriptions.eq(i)
    columns = subscription.find('td')
    if columns.length > 0
      checkbox = columns.eq(0).find('.filled-in')
      feed_id = checkbox.attr('name')
      title = columns.eq(1).text()
      if feed_to_find == feed_id
        return title

showSelectedFeedList = (feedListToShow, feedListToHide, isDetailed) ->
  clearAndShow(feedListToShow)
  fillWithSelectedFeeds(feedListToShow, isDetailed)
  feedListToHide.parent().hide()
  feedListToShow.parent().show()

fillWithSelectedFeeds = (elementToFill, isDetailed) ->
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
        if isDetailed
          elementToFill.append(detailedFeedListItem(feed_id, title, i))
        else
          elementToFill.append(simpleFeedListItem(feed_id, title))

showSubscriptionFeedList = (subscription, feedListToShow, feedListToHide, isDetailed) ->
  clearAndShow(feedListToShow)
  if subscription['feeds']?
    for i in [0...subscription['feeds'].length]
      item = subscription['feeds'][i]
      feedId = item['id']
      title = item['title']
      if title == null
        title = findFeedTitle(feedId)
      if title != null
        if isDetailed
          feedListToShow.append(
            detailedFeedListItem(feedId, title, i, item['includeImages'], item['markAsRead'], item['fullArticle'])
          )
        else
          feedListToShow.append(simpleFeedListItem(feedId, title))
  feedListToShow.parent().show()
  feedListToHide.parent().hide()

recoverSubscriptionFromModal = (detailed) ->
  subscription = { }
  time = null
  timezone = null
  if detailed
    time = $('#subscription_edit_times_detailed option:selected').text()
    timezone = $('#subscription_edit_timezones_detailed option:selected').text()
  else
    time = $('#subscription_edit_times_simple option:selected').text()
    timezone = $('#subscription_edit_timezones_simple option:selected').text()
#  for i in [0...options.length]
#    option = options[i]
#    if option.selected == "selected"
#      timezone = option.value
#      break

  subscription['time'] = time
  subscription['timezone'] = timezone
  feeds = [ ]
  elems = null
  if (detailed)
    elems = $(SUBSCRIPTION_EDIT_DETAILED_FEED_LIST_SELECTOR).find('div')
  else
    elems = $(SUBSCRIPTION_EDIT_FEED_LIST_SELECTOR).find('div')
  elems.each ->
    feed = $(@)
    item = {}
    item['id'] = feed.attr('feed_id')
    item['title'] = feed.attr('title')
    feeds.push(item)
  subscription['feeds'] = feeds
  return subscription


simpleFeedListItem = (feed_id, title) ->
    "<li class='collection-item'>
    <div feed_id='#{feed_id}' title='#{title}'>
    #{title}
    <a href='#!' class='secondary-content self_remove'>
        <i class='material-icons'>clear</i>
    </a>
    </div></li>"

detailedFeedListItem = (feed_id, title, id) ->
  detailedFeedListItem(feed_id, title, id, false, true, true)

detailedFeedListItem = (feed_id, title, id, include_images, mark_as_read, full_article) ->
  includeImagesCheckbox = checkbox('include_images', 'includeImages' + id, include_images)
  marAsReadCheckbox = checkbox('mark_as_read', 'mark_as_read' + id, mark_as_read)
  fullArticleCheckbox = checkbox('full_article', 'full_article' + id, full_article)
  "<li class='collection-item'>
  <div feed_id='#{feed_id}' title='#{title}'>
      #{title}
      <a href='#!' class='secondary-content self_remove'>
          <i class='material-icons'>clear</i>
      </a>
      <p>
  " + includeImagesCheckbox +
  "      <label for='include_images#{id}'>Include images</label>
        </p>
        <p>" + marAsReadCheckbox + "
        <label for='mark_as_read#{id}'>Mark as read</label>
        </p>
        <p>" + fullArticleCheckbox + "
        <label for='full_article#{id}'>Full article</label>
      </p>
      </div></li>"

checkbox = (clazz, id, checked) ->
  if checked
    "<input type='checkbox' class='filled-in #{clazz}' id='#{id}'/>"
  else
    "<input type='checkbox' class='filled-in #{clazz}' id='#{id}' checked='checked'/>"

clearAndShow = (elem) ->
  elem.empty()
  elem.show()

enableButton = (button) ->
  button.removeClass('disabled')

emptyListPlaceholder = ->
  "<div class='error_div'>No feeds selected</div>"

removeEmptyListPlaceholder = ->
  $('.error_div').remove()

initTimezone = ->
  tz = jstz.determine();
  setTimezone(tz.name())

setTimezone = (tz) ->
  timezones = $('.timezones').find('option')
  for i in [0...timezones.length]
    timezone = timezones[i]
    if timezone.value == tz
        timezone.selected = "selected"

setTime = (t) ->
  times = $('.times').find('option')
  for i in [0...times.length]
    time = times[i]
    if time.value == t
        time.selected = "selected"

isEmpty = (elem) ->
  elem.children().length == 0
