const React = require('react');

const WeatherForm = React.createClass({

    handleSubmit: function (e) {
        e.preventDefault();

        const location = this.refs.location.value;
        if (location.length > 0) {
            this.refs.location.value = '';

            this.props.onSearch(location);
        }
    },

    render: function () {
        return (
            <div>
                <form action="demo_form.asp" onSubmit={this.handleSubmit}>
                    <input type="text" placeholder="Enter city name" ref="location"/>
                    <button className="button expanded hollow">Get Weather</button>
                </form>
            </div>
        );
    }
});

module.exports = WeatherForm;