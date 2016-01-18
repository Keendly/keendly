var FeedBox = React.createClass({
  loadFeeds: function() {
    $.ajax({
      url: this.props.url,
      dataType: 'json',
      cache: false,
      success: function(data) {
        this.setState({data: data});
      }.bind(this),
      error: function(xhr, status, err) {
        window.location.replace("login?error=Log in first");
      }.bind(this)
    });
  },
  getInitialState: function() {
    return {data: []};
  },
  componentDidMount: function() {
    this.loadFeeds();
  },
  render: function() {
    return (
      <div className="container" id="subs-container">
        <div className="row">
          <div className="col s12 m6">
            <a className="waves-effect waves-light btn modal-trigger" id="delivery_modal_btn" href="#delivery_modal">Deliver now</a>
            <a className="waves-effect waves-light btn modal-trigger" id="subscription_modal_btn" href="#subscription_modal">Schedule</a>
          </div>
          <div className="input-field col offset-m3 s12 m3" id="search">
            <input id="search_box" type="search" required />
            <label htmlFor="search_box"><i className="material-icons">search</i></label>
          </div>
        </div>
        <FeedList data={this.state.data} />
      </div>
    );
  }
});

var FeedList = React.createClass({
  render: function() {
  var feedNodes = this.props.data.map(function(feed) {
    return (
      <Feed title={feed.title} key={feed.feedId} lastDelivery={feed.lastDelivery}>

      </Feed>
    );
    });
    return (
      <table className="highlight" id="subscriptions">
        <thead>
        <tr>
          <th></th>
          <th>Title</th>
          <th>Last delivery</th>
        </tr>
        </thead>

        <tbody>
          {feedNodes}
        </tbody>
      </table>
    );
  }
});

var Feed = React.createClass({
  render: function() {
    return (
      <tr>
        <td><input type="checkbox" className="filled-in" /></td>
        <td className="feed_title">{this.props.title}</td>
        <td>{this.props.lastDelivery != null ? moment(this.props.lastDelivery.deliveryDate).fromNow() : ''}</td>
      </tr>
    );
  }
});

ReactDOM.render(
  <FeedBox url="api/feeds" />,
  document.getElementById('content')
);
