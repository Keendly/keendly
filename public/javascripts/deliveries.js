var DeliveryBox = React.createClass({
  loadFeeds: function() {
    $.ajax({
      url: this.props.url + '?page=1&pageSize=10',
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
      <div className="container">
        <DeliveryList data={this.state.data} />
      </div>
    );
  }
});

var DeliveryList = React.createClass({
  render: function() {
    var deliveryNodes = this.props.data.map(function(delivery) {
      return (
        <Delivery items={delivery.items} key={delivery.id} date={delivery.deliveryDate} />
      );
    });
    return (
      <table id="deliveries">
        <thead>
        <tr>
          <th>Feeds</th>
          <th>Delivery date</th>
        </tr>
        </thead>

        <tbody>
          {deliveryNodes}
        </tbody>
      </table>
    );
  }
});

var Delivery = React.createClass({
  render: function() {
    if (this.props.items != null){
      var items = this.props.items.map(function(item) {
        return (
          item.title
        );
      });
    }
    return (
      <tr>
        <td>
            {items != null ? items : ''}
        </td>
        <td>
            {this.props.date != null ? moment(this.props.date).format('llll') : ''}
        </td>
      </tr>
    );
  }
});

ReactDOM.render(
  <DeliveryBox url="api/deliveries" />,
  document.getElementById('content')
);
