var UserForm = React.createClass({
  handleSubmit: function (event) {
    event.preventDefault();
    $.ajax({
      url: this.props.url,
      type: 'PATCH',
      data: JSON.stringify({"deliveryEmail": this.state.deliveryEmail}),
      contentType: "application/json; charset=utf-8",
      cache: false,
      success: function() {
        this.setState({ success: true, error: false})
      }.bind(this),
      error: function(xhr, status, err) {
        this.setState({ error: true, success: false})
      }.bind(this)
    });
  },
  handleChange: function(event) {
    this.setState({
      deliveryEmail: event.target.value
    });
  },
  getInitialState: function() {
    return {deliveryEmail: '', error: false, success: false};
  },
  componentDidMount: function() {
   $.get(this.props.url, function(data) {
      if (this.isMounted()) {
        this.setState({
          deliveryEmail: data.deliveryEmail
        });
      }
    }.bind(this));
  },
  render: function() {
    return (
     <div className="container">
      {this.state.error == true ? <div className='error_div'>Error occured while saving.</div> : ''}
      {this.state.success == true ? <div className='success_div'>Email saved.</div> : ''}
      <div className='info_div'>Remember to add <i>kindle@keendly.com</i> to your <b>Approved Personal Document E-mail List</b>, you can do it <a href='https://www.amazon.com/mn/dcw/myx.html/ref=kinw_myk_surl_2#/home/settings/' target="_blank">here</a>.</div>
      <form className="col s12" id="settings_form" method="POST" onSubmit={this.handleSubmit}>
          <div className="row">
              <div className="input-field col s12">
                <input value={this.state.deliveryEmail} onChange={this.handleChange} name="email" id="email" type="email" className="validate"/>
                <label className="active" htmlFor="email">Send-to-Kindle E-Mail</label>
            </div>
          </div>
          <button className="btn waves-effect waves-light" type="submit" name="action">Save
              <i className="material-icons right">send</i>
          </button>
      </form>
      </div>
    );
  }
});

ReactDOM.render(
  <UserForm url="api/users/self"/>,
  document.getElementById('content')
);
