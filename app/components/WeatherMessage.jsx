const React = require('react');

const WeatherMessage = ({temp, location}) => <div>
    <h3 className="text-center">Weather in {location} is {temp}!</h3>
</div>;

module.exports = WeatherMessage;