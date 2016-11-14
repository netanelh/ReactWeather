const React = require('react');
const Nav = require('./Nav');

const Main = (props) => {
    return (
        <div>
            <Nav/>
            <div className="row">
                <div className="columns large-4 medium-6 small-centered">
                    {props.children}
                </div>
            </div>
        </div>
    );
};

module.exports = Main;