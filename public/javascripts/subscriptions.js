var DeliveryBox = React.createClass({
  loadSubscriptions: function(page) {
    var token = this.getCookie('k33ndly_535510n');
    $.ajax({
      url: this.props.url + '?page=' + page +'&pageSize=20',
      headers: {
        'Authorization': token
      },
      dataType: 'json',
      cache: false,
      success: function(data) {
        this.setState({data: data});
      }.bind(this),
      error: function(xhr, status, err) {
        window.location.replace("login");
      }.bind(this)
    });
  },
  getInitialState: function() {
    return {data: [], page: 1};
  },
  componentDidMount: function() {
    this.loadSubscriptions(this.state.page);
  },
  handlePageClick: function(newPage) {
    this.setState({
      page: newPage
    });
    this.loadSubscriptions(newPage);
  },
  deleteButtonClick: function() {
    var timestamp = Date.now()
    ReactDOM.render(
      <DeleteModal url='api/subscriptions' key={timestamp} success={this.handleSuccess} error={this.handleError}/>,
      document.getElementById('modal')
    );
    $('#delete_modal').openModal()
  },
  handleSuccess: function() {
    this.setState({success: true, error: false})
    this.loadSubscriptions(this.state.page)
  },
  handleError: function(code, description) {
     this.setState({success: false, error: true, errorDescription: description})
  },
  getCookie: function (a) {
      var b = document.cookie.match('(^|;)\\s*' + a + '\\s*=\\s*([^;]+)');
      return b ? b.pop() : '';
  },
  render: function() {
    return (
      <div className="container">
        {this.state.success == true ? <div className='success_div'>Subscription(s) deleted.</div> : ''}
        {this.state.error == true ? <div className='error_div'>{this.state.errorDescription}</div> : ''}
        <div className="row" id="button_row">
          <div className="col s12 m6">
            <a onClick={this.deleteButtonClick} className="waves-effect waves-light btn" id="delete_subscription_btn" href="#delete_modal">Delete</a>
          </div>
        </div>
        <SubscriptionList data={this.state.data} />
        <Pagination handleClick={this.handlePageClick} page={this.state.page} />
      </div>
    );
  }
});

var DeleteModal = React.createClass({
  getInitialState: function() {
    return {subscriptions: this.getSelectedSubscriptions()};
  },
  handleSubmit: function() {
    var token = this.getCookie('k33ndly_535510n');
     var subscriptionsLength = this.state.subscriptions.length;
     var error = false;
     var errorDescription;
     for (var i = 0; i < subscriptionsLength; i++){
           $.ajax({
             url: this.props.url + "/" + this.state.subscriptions[i],
             type: "DELETE",
              headers: {
                'Authorization': token
              },
             success: function(data) {
               this.props.success()
             }.bind(this),
             error: function(xhr, status, err) {
               error = true;
               errorDescription = xhr.responseJSON.description;
             }.bind(this)
           });
     }
     $('#delete_modal').closeModal();
     if (error == false){
        this.props.success()
     } else {
        this.props.error(errorDescription)
     }
  },
  getCookie: function (a) {
      var b = document.cookie.match('(^|;)\\s*' + a + '\\s*=\\s*([^;]+)');
      return b ? b.pop() : '';
  },
  getSelectedSubscriptions: function() {
    var checkbox, columns, id, i, j, ref, results, subscription, subscriptions, subscriptionsLength, title;
    subscriptions = $('#subscriptions').find('tr');
    subscriptionsLength = subscriptions.length;
    results = [];
    for (i = j = 0, ref = subscriptionsLength; 0 <= ref ? j < ref : j > ref; i = 0 <= ref ? ++j : --j) {
      subscription = subscriptions.eq(i);
      columns = subscription.find('td');
      if (columns.length > 0) {
        checkbox = columns.eq(0).find('.filled-in');
        if (checkbox.is(':checked')) {
          id = checkbox.attr('id');
          results.push(id);
        }
      }
    }
    return results;
  },
  render: function() {
    var subscriptions = this.state.subscriptions;
    if (subscriptions.length == 0){
      return (
          <div id="delete_modal" className="modal">
            <div className="error_modal">
              Select subscriptions first
              </div>
          </div>
        )
    }


    return (
      <div id="delete_modal" className="modal">
          <div className="modal-content" id="delivery_form">
              <h4>Are you sure?</h4>
              Do you want to remove selected subscriptions?
          </div>
          <div className="modal-footer">
              <a href="#!" onClick={this.handleSubmit} className="modal-action waves-effect waves-green btn-flat submit save" id="subscription_save_btn">Yes</a>
              <a href="#!" className="modal-action modal-close waves-effect waves-red btn-flat">No</a>
          </div>
      </div>
    );
  }
});

var SubscriptionList = React.createClass({
  render: function() {
    var subscriptions = this.props.data.map(function(subscription) {
      return (
        <Subscription feeds={subscription.feeds} id={subscription.id} key={subscription.id} time={subscription.time} timezone={subscription.timezone} />
      );
    });
    return (
      <table id="subscriptions">
        <thead>
        <tr>
          <th></th>
          <th>Feeds</th>
          <th>Delivery time</th>
        </tr>
        </thead>

        <tbody>
          {subscriptions}
        </tbody>
      </table>
    );
  }
});

var Subscription = React.createClass({
  render: function() {
    if (this.props.feeds != null){
      var feeds = this.props.feeds.map(function(feed) {
        return (
          feed.title
        );
      });
    }
    return (
      <tr>
        <td>
          <input type="checkbox" className="filled-in" id={this.props.id} />
          <label htmlFor={this.props.id}></label>
        </td>
        <td>
            {feeds != null ? feeds.join(" \u2022 ") : ''}
        </td>
        <td>
            {this.props.time} ({this.props.timezone})
        </td>
      </tr>
    );
  }
});

var Pagination = React.createClass({
  handleClick: function(page) {
    this.props.handleClick(page);
  },
  render: function() {
    var currentPage = this.props.page;
    var left = currentPage == 1 ?
                  <li className="disabled"><i className="material-icons">chevron_left</i></li> :
                  <li className="waves-effect"><i onClick={this.handleClick.bind(this, currentPage-1)} className="material-icons">chevron_left</i></li>

    var numbers = [0,1,2,3].map(function(i) {
      if (currentPage == 1){
        if (i == 0){
          return <PageNumber handleClick={this.handleClick} active='true' page={currentPage} key={i} />
        } else {
          return <PageNumber handleClick={this.handleClick}  active='false' page={i + currentPage} key={i} />
        }
      }
      if (currentPage == 2){
        if (i == 1){
          return <PageNumber handleClick={this.handleClick}  active='true' page={currentPage} key={i} />
        } else {
          return <PageNumber handleClick={this.handleClick}  active='false' page={i + currentPage - 1} key={i} />
        }
      }
      if (currentPage > 2){
        if (i == 2){
          return <PageNumber handleClick={this.handleClick}  active='true' page={currentPage} key={i} />
        } else {
          return <PageNumber handleClick={this.handleClick}  active='false' page={i + currentPage - 2} key={i} />
        }
      }
    }, this);
    var right = <li className="waves-effect"><i onClick={this.handleClick.bind(this, currentPage+1)} className="material-icons">chevron_right</i></li>
    return (
      <ul className="pagination">
        {left}
        {numbers}
        {right}
      </ul>
    );
  }
});

var PageNumber = React.createClass({
  handleClick: function(page) {
    this.props.handleClick(page);
  },
  render: function() {
    var active = this.props.active;
    var page = this.props.page;
    if (active == 'true'){
      return <li onClick={this.handleClick.bind(this, page)} className="active">{page}</li>
    } else {
      return <li onClick={this.handleClick.bind(this, page)} className="waves-effect">{page}</li>
    }
  }
})

ReactDOM.render(
  <DeliveryBox url="https://m1ndoce0cl.execute-api.eu-west-1.amazonaws.com/v1/subscriptions" />,
  document.getElementById('content')
);
