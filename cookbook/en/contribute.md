# How to Contribute

Thank you for your interest in the AgentScope Runtime Java project.

AgentScope Runtime Java is an open-source project focused on agent deployment and secure tool execution, with a friendly developer community that is happy to help new contributors. We welcome all types of contributions, from code improvements to documentation writing.

## Community

The first step to participating in the AgentScope Runtime Java project is to join our discussions and connect with us through different communication channels. Here are several ways to get in touch:

- **GitHub Discussions**: Ask questions and share experiences (please use **English**)
- **Discord**: Join our [Discord channel](https://discord.gg/eYMpfnkG8h) for real-time discussions
- **DingTalk**: Chinese users can join our [DingTalk group](https://qr.dingtalk.com/action/joingroup?code=v1,k1,OmDlBXpjW+I2vWjKDsjvI9dhcXjGZi3bQiojOq3dlDw=&_dt_no_comment=1&origin=11)

## Reporting Issues

### Bugs

If you find a bug in AgentScope Runtime Java, please first test with the latest version to ensure your issue hasn't been fixed. If not, please search our issue list on GitHub to see if a similar issue has already been raised.

- If you confirm the bug hasn't been reported, please submit a bug issue before writing any code. When submitting an issue, please include:
- Clear problem description
- Reproduction steps
- Code/error messages
  - Environment details (operating system, JDK version)

- Affected components (e.g., Engine module, Sandbox module, etc.)

### Security Issues

If you discover a security issue in AgentScope Runtime Java, please report it to us through the [Alibaba Security Response Center (ASRC)](https://security.alibaba.com/).

## Feature Requests

If you would like AgentScope Runtime Java to have a feature that doesn't exist, please submit a feature request issue on GitHub, describing:

- The feature and its purpose
- How it should work
- Security considerations (if applicable)

## Contributing Code

If you want to contribute new features or bug fixes to AgentScope Runtime Java, please first discuss your ideas in a GitHub issue. If no related issue exists, please create one. Someone may already be working on it, or it may have special complexities (especially security considerations for Sandbox features) that you should understand before starting to code.

### Fork and Create Branch

Fork the [AgentScope Runtime Java main branch code](https://github.com/agentscope-ai/agentscope-runtime-java) and clone it to your local machine. For help, see the GitHub help pages.

Create a branch with a descriptive name.

```bash
git checkout -b feature/your-feature-name
```

### Make Changes

- Write clear, well-commented code
- Follow existing code style
- Add tests for new features/fixes
- Update documentation as needed Test Your Changes

### Test Your Changes

Run the test suite to ensure your changes don't break existing functionality:

```bash
mvn test
```

### Submit Your Changes

1. Commit your changes with a clear message:

```bash
git commit -m "Add: brief description of your changes"
```

2. Push to your Fork:

```bash
git push origin feature/your-feature-name
```

3. Create a Pull Request (PR) from your branch to the main repository with a **clear description**

### Code Review Process

- All PRs require maintainer review
- Address any feedback or requested changes
- Once approved, your PR will be merged

Thank you for contributing to AgentScope Runtime Java!
