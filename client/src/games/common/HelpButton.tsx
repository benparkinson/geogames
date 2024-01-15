import React, { useEffect, useRef, useState } from 'react';
import { Button, Overlay, Tooltip } from 'react-bootstrap';

const HelpButton = ({ giveUp, openExplanationModal }): JSX.Element => {
    const [showHelp, setShowHelp] = useState(false);
    const target = useRef(null);

    function handleExplanationClick() {
        setShowHelp(false);
        openExplanationModal();
    }

    return (
        <div className="help-button-container">
            <Button ref={target} variant="secondary" onClick={() => setShowHelp(!showHelp)}>?</Button>
            <Overlay target={target.current} show={showHelp} placement="bottom">
                {(props) => (
                    <Tooltip {...props}>
                        <div className="m-1 d-flex justify-content-center">
                            <Button variant="secondary" onClick={giveUp} >Give up</Button>
                        </div>
                        <div className="m-1 d-flex justify-content-center">
                            <Button variant="info" onClick={handleExplanationClick} >Game explanation</Button>
                        </div>
                    </Tooltip>
                )}
            </Overlay>
        </div>
    );
};

export default HelpButton;
