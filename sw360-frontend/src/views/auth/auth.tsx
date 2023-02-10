import React, { useState, useEffect, useContext } from 'react';
import './css/auth.css'
import VnIcon from '../../assets/images/vi.png';
import EnIcon from '../../assets/images/en.png';
import JpIcon from '../../assets/images/jp.png';
import Modal from 'react-bootstrap/Modal';
import Button from 'react-bootstrap/Button';
import Form from 'react-bootstrap/Form';
import Alert from 'react-bootstrap/Alert';
import AuthService from '../../services/auth.service';
import UserData from '../../object-types/UserData';
import { useNavigate, Navigate } from "react-router-dom";
import { PublicContext } from '../../contexts/public.context';

const AuthScreen = () => {
    const { isAuthenticated, setIsAuthenticated } = useContext(PublicContext);
    const navigate = useNavigate();
    const [dialogShow, setDialogShow] = useState<boolean>(false);
    const [messageShow, setMessageShow] = useState<boolean>(false);
    const [emailAddress, setEmailAddress] = useState<string>("sw360.org");
    const [password, setPassword] = useState<string>("");
    const handleClose = () => setDialogShow(false);
    const handleShow = () => setDialogShow(true);

    useEffect(() => {
        if (isAuthenticated) {
            navigate('/');
        }
    }, [isAuthenticated]);

    const handleLogin = async () => {
        let userData: UserData = { emailAddress: emailAddress, password: password }
        let token = await AuthService.generateToken(userData);

        if (token == null) {
            setMessageShow(true);
            setIsAuthenticated(false);
            return;
        }
        setIsAuthenticated(true);
        navigate('/');
    };

    return (
        <>
            <section className="portlet" id="portlet_sw360_portlet_welcome">
                <div>
                    <div className="autofit-float autofit-row portlet-header">
                        <div className="autofit-col autofit-col-expand">
                            <h2 className="portlet-title-text">Welcome</h2>
                        </div>
                        <div className="autofit-col autofit-col-end">
                            <div className="autofit-section">
                            </div>
                        </div>
                    </div>
                    <div className="portlet-content-container p-1" style={{ background: "#f1f2f5" }}>
                        <div className="portlet-body p-5">
                            <div className="jumbotron">
                                <h1 className="display-4">Welcome to SW360!</h1>
                                <a href="/en_US" title="English"> <img src={EnIcon} width="25px" height="25px" /></a>
                                <a href="/ja_JP" title="Japan"> <img src={JpIcon} width="25px" height="25px" /></a>
                                <a href="/vi_VN" title="Vietnam"> <img src={VnIcon} width="25px" height="25px" /></a>
                                <br />
                                <p className="mt-3">
                                    SW360 is an open source software project that provides both a web application and a repository to collect, organize and make available information about software components. It establishes a central hub for software components in an organization.
                                </p>
                                <hr className="my-4" />
                                <h3>In order to go ahead, please sign in or create a new account!</h3>
                                <div className="buttons">
                                    <span className="sign-in">
                                        <a className="btn btn-primary btn-lg" role="button" onClick={handleShow}>Sign In</a>
                                    </span>
                                    <a className="btn btn-outline-primary btn-lg" style={{ marginLeft: "3rem" }} role="button">Create Account</a>
                                </div>

                            </div>

                        </div>
                    </div>
                </div>
            </section>

            <Modal
                show={dialogShow}
                onHide={handleClose}
                backdrop="static"
                className='login-modal'
                centered
            >
                <Modal.Header closeButton>
                    <Modal.Title>Sign in</Modal.Title>
                </Modal.Header>
                <Modal.Body>
                    <Alert variant='danger' onClose={() => setMessageShow(false)} dismissible show={messageShow}>
                        Authentication failed. Please try again.
                    </Alert>
                    <Form>
                        <Form.Group className="mb-3">
                            <Form.Label>Email address</Form.Label>
                            <Form.Control
                                type="email"
                                defaultValue="sw360.org"
                                onChange={event => setEmailAddress(event.target.value)}
                                autoFocus
                                required
                            />
                            <Form.Control.Feedback type="invalid">
                                Please enter a valid email address.
                            </Form.Control.Feedback>
                        </Form.Group>
                        <Form.Group
                            className="mb-3"
                        >
                            <Form.Label>Password</Form.Label>
                            <Form.Control
                                type="password"
                                placeholder=""
                                onChange={event => setPassword(event.target.value)}
                                required
                            />
                            <Form.Control.Feedback type="invalid">
                                This field is required.
                            </Form.Control.Feedback>
                        </Form.Group>
                    </Form>
                </Modal.Body>
                <Modal.Footer className="justify-content-start" >
                    <Button className="login-btn" variant="primary" onClick={handleLogin}> Sign in </Button>
                </Modal.Footer>
            </Modal>
        </>
    );
}

export default AuthScreen